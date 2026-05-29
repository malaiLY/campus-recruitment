package com.campus.recruitment.module.job.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.CompanyAuditStatus;
import com.campus.recruitment.common.enums.JobStatus;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.JobFavorite;
import com.campus.recruitment.entity.JobTag;
import com.campus.recruitment.mapper.CompanyProfileMapper;
import com.campus.recruitment.mapper.JobFavoriteMapper;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.JobTagMapper;
import com.campus.recruitment.module.job.dto.CreateJobRequest;
import com.campus.recruitment.module.job.dto.JobQueryDTO;
import com.campus.recruitment.module.job.dto.UpdateJobRequest;
import com.campus.recruitment.module.job.service.JobService;
import com.campus.recruitment.module.job.vo.JobDetailVO;
import com.campus.recruitment.module.job.vo.JobVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final JobFavoriteMapper jobFavoriteMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public PageResult<JobVO> listJobs(JobQueryDTO queryDTO) {
        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Job::getStatus, JobStatus.PUBLISHED.name());

        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
            String keyword = queryDTO.getKeyword().trim();
            queryWrapper.and(w -> w.like(Job::getTitle, keyword)
                    .or().like(Job::getCategory, keyword)
                    .or().like(Job::getCity, keyword));
        }

        if (queryDTO.getCity() != null && !queryDTO.getCity().trim().isEmpty()) {
            queryWrapper.eq(Job::getCity, queryDTO.getCity().trim());
        }

        if (queryDTO.getSalaryMin() != null) {
            queryWrapper.ge(Job::getSalaryMax, queryDTO.getSalaryMin());
        }

        if (queryDTO.getSalaryMax() != null) {
            queryWrapper.le(Job::getSalaryMin, queryDTO.getSalaryMax());
        }

        if (queryDTO.getEducation() != null && !queryDTO.getEducation().trim().isEmpty()) {
            queryWrapper.eq(Job::getEducation, queryDTO.getEducation().trim());
        }

        if ("HOT".equals(queryDTO.getSort())) {
            queryWrapper.orderByDesc(Job::getViewCount, Job::getApplyCount);
        } else {
            queryWrapper.orderByDesc(Job::getCreateTime);
        }

        Page<Job> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        jobMapper.selectPage(page, queryWrapper);

        List<Job> jobs = page.getRecords();
        if (jobs.isEmpty()) {
            return new PageResult<>(List.of(), 0, queryDTO.getPageNum(), queryDTO.getPageSize());
        }

        List<Long> companyIds = jobs.stream().map(Job::getCompanyId).distinct().collect(Collectors.toList());
        List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());

        Map<Long, CompanyProfile> companyMap = companyProfileMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(CompanyProfile::getId, c -> c));

        Map<Long, List<JobTag>> tagMap = jobTagMapper.selectList(
                new LambdaQueryWrapper<JobTag>().in(JobTag::getJobId, jobIds))
                .stream().collect(Collectors.groupingBy(JobTag::getJobId));

        List<JobVO> voList = jobs.stream().map(job -> {
            JobVO vo = convertToJobVO(job);
            CompanyProfile company = companyMap.get(job.getCompanyId());
            if (company != null) {
                vo.setCompanyName(company.getCompanyName());
            }
            List<JobTag> tags = tagMap.get(job.getId());
            vo.setTags(tags != null ? tags.stream().map(JobTag::getTagName).collect(Collectors.toList()) : List.of());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public JobDetailVO getJobDetail(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_EXIST);
        }

        jobMapper.incrementViewCount(id);
        job = jobMapper.selectById(id);

        JobDetailVO vo = convertToJobDetailVO(job);

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getId, job.getCompanyId()));
        if (company != null) {
            vo.setCompanyName(company.getCompanyName());
        }

        vo.setTags(getTagsByJobId(job.getId()));

        Long userId = LoginUserContext.getUserId();
        if (userId != null) {
            Long count = jobFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<JobFavorite>()
                            .eq(JobFavorite::getStudentId, userId)
                            .eq(JobFavorite::getJobId, id));
            vo.setFavorited(count > 0);
        } else {
            vo.setFavorited(false);
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createJob(CreateJobRequest request) {
        Long userId = LoginUserContext.getUserId();
        verifyCompanyAuthorization(userId);

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));

        Job job = new Job();
        BeanUtils.copyProperties(request, job);
        job.setCompanyId(company.getId());
        job.setStatus(JobStatus.DRAFT.name());
        job.setViewCount(0);
        job.setApplyCount(0);
        job.setFavoriteCount(0);
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        job.setCreateBy(userId);
        job.setUpdateBy(userId);

        jobMapper.insert(job);

        saveTags(job.getId(), request.getTags());

        return job.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateJob(Long id, UpdateJobRequest request) {
        Long userId = LoginUserContext.getUserId();
        Job job = getJobByIdAndCheckOwner(id, userId);

        if (!JobStatus.DRAFT.name().equals(job.getStatus())
                && !JobStatus.REJECTED.name().equals(job.getStatus())
                && !JobStatus.OFFLINE.name().equals(job.getStatus())) {
            throw new BizException(ErrorCode.JOB_STATUS_INVALID);
        }

        BeanUtils.copyProperties(request, job);
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdateBy(userId);

        jobMapper.updateById(job);

        jobTagMapper.delete(new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId, id));
        saveTags(id, request.getTags());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForReview(Long id) {
        Long userId = LoginUserContext.getUserId();
        Job job = getJobByIdAndCheckOwner(id, userId);

        if (!JobStatus.DRAFT.name().equals(job.getStatus())
                && !JobStatus.REJECTED.name().equals(job.getStatus())) {
            throw new BizException(ErrorCode.JOB_STATUS_INVALID);
        }

        job.setStatus(JobStatus.PENDING_REVIEW.name());
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdateBy(userId);
        jobMapper.updateById(job);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineJob(Long id) {
        Long userId = LoginUserContext.getUserId();
        Job job = getJobByIdAndCheckOwner(id, userId);

        if (!JobStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BizException(ErrorCode.JOB_STATUS_INVALID);
        }

        job.setStatus(JobStatus.OFFLINE.name());
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdateBy(userId);
        jobMapper.updateById(job);

        syncJobStatusToEs(id, "OFFLINE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteJob(Long jobId) {
        Long userId = LoginUserContext.getUserId();

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_EXIST);
        }

        Long existCount = jobFavoriteMapper.selectCount(
                new LambdaQueryWrapper<JobFavorite>()
                        .eq(JobFavorite::getStudentId, userId)
                        .eq(JobFavorite::getJobId, jobId));
        if (existCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "已收藏该岗位");
        }

        JobFavorite favorite = new JobFavorite();
        favorite.setStudentId(userId);
        favorite.setJobId(jobId);
        favorite.setFavoriteTime(LocalDateTime.now());
        favorite.setCreateTime(LocalDateTime.now());
        jobFavoriteMapper.insert(favorite);

        jobMapper.incrementFavoriteCount(jobId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteJob(Long jobId) {
        Long userId = LoginUserContext.getUserId();

        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_EXIST);
        }

        int deleted = jobFavoriteMapper.delete(
                new LambdaQueryWrapper<JobFavorite>()
                        .eq(JobFavorite::getStudentId, userId)
                        .eq(JobFavorite::getJobId, jobId));

        if (deleted > 0) {
            jobMapper.decrementFavoriteCount(jobId);
        }
    }

    @Override
    public PageResult<JobVO> getMyJobs(String status, Integer pageNum, Integer pageSize) {
        Long userId = LoginUserContext.getUserId();
        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (company == null) {
            return new PageResult<>(new ArrayList<>(), 0, pageNum, pageSize);
        }

        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Job::getCompanyId, company.getId());

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq(Job::getStatus, status.trim());
        }

        queryWrapper.orderByDesc(Job::getCreateTime);

        Page<Job> page = new Page<>(pageNum, pageSize);
        jobMapper.selectPage(page, queryWrapper);

        List<Job> jobs = page.getRecords();
        if (jobs.isEmpty()) {
            return new PageResult<>(new ArrayList<>(), 0, pageNum, pageSize);
        }

        List<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toList());
        Map<Long, List<JobTag>> tagMap = jobTagMapper.selectList(
                new LambdaQueryWrapper<JobTag>().in(JobTag::getJobId, jobIds))
                .stream().collect(Collectors.groupingBy(JobTag::getJobId));

        List<JobVO> voList = jobs.stream().map(job -> {
            JobVO vo = convertToJobVO(job);
            vo.setCompanyName(company.getCompanyName());
            List<JobTag> tags = tagMap.get(job.getId());
            vo.setTags(tags != null ? tags.stream().map(JobTag::getTagName).collect(Collectors.toList()) : List.of());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void verifyCompanyAuthorization(Long userId) {
        String userType = LoginUserContext.get().getUserType();
        if (!UserType.COMPANY.name().equals(userType)) {
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (company == null || !CompanyAuditStatus.APPROVED.name().equals(company.getAuditStatus())) {
            throw new BizException(ErrorCode.COMPANY_UNVERIFIED);
        }
    }

    private Job getJobByIdAndCheckOwner(Long jobId, Long userId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_EXIST);
        }

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (company == null || !company.getId().equals(job.getCompanyId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该岗位");
        }

        return job;
    }

    private JobVO convertToJobVO(Job job) {
        JobVO vo = new JobVO();
        BeanUtils.copyProperties(job, vo);
        return vo;
    }

    private JobDetailVO convertToJobDetailVO(Job job) {
        JobDetailVO vo = new JobDetailVO();
        BeanUtils.copyProperties(job, vo);
        return vo;
    }

    private List<String> getTagsByJobId(Long jobId) {
        List<JobTag> tags = jobTagMapper.selectList(
                new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId, jobId));
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream().map(JobTag::getTagName).collect(Collectors.toList());
    }

    private void saveTags(Long jobId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        List<JobTag> tags = tagNames.stream().map(name -> {
            JobTag tag = new JobTag();
            tag.setJobId(jobId);
            tag.setTagName(name);
            tag.setCreateTime(LocalDateTime.now());
            return tag;
        }).collect(Collectors.toList());
        tags.forEach(jobTagMapper::insert);
    }

    private void syncJobStatusToEs(Long jobId, String status) {
        try {
            String messageId = java.util.UUID.randomUUID().toString();
            Map<String, Object> message = Map.of(
                    "messageId", messageId,
                    "action", "update",
                    "jobId", jobId
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.JOB_ES_EXCHANGE,
                    RabbitMQConstants.JOB_ES_ROUTING_KEY,
                    message
            );
            log.info("发送岗位ES同步MQ消息: messageId={}, jobId={}, action=update", messageId, jobId);
        } catch (Exception e) {
            log.error("发送岗位ES同步MQ消息失败: {}", e.getMessage(), e);
        }
    }
}
