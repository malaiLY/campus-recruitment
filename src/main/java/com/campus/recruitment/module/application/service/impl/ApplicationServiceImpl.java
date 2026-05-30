package com.campus.recruitment.module.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.ApplicationStatus;
import com.campus.recruitment.common.enums.JobStatus;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.mq.OutboxService;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.ApplicationLog;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.JobApplication;
import com.campus.recruitment.entity.Resume;
import com.campus.recruitment.entity.StudentProfile;
import com.campus.recruitment.entity.Message;
import com.campus.recruitment.mapper.ApplicationLogMapper;
import com.campus.recruitment.mapper.CompanyProfileMapper;
import com.campus.recruitment.mapper.JobApplicationMapper;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.MessageMapper;
import com.campus.recruitment.mapper.ResumeMapper;
import com.campus.recruitment.mapper.StudentProfileMapper;
import com.campus.recruitment.module.application.dto.CreateApplicationRequest;
import com.campus.recruitment.module.application.dto.UpdateApplicationStatusRequest;
import com.campus.recruitment.module.application.vo.ApplicationDetailVO;
import com.campus.recruitment.module.application.vo.ApplicationStatusLogVO;
import com.campus.recruitment.module.application.service.ApplicationService;
import com.campus.recruitment.module.application.vo.ApplicationStatusVO;
import com.campus.recruitment.module.application.vo.ApplicationVO;
import com.campus.recruitment.module.application.vo.MyApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final JobApplicationMapper jobApplicationMapper;
    private final ApplicationLogMapper applicationLogMapper;
    private final JobMapper jobMapper;
    private final ResumeMapper resumeMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final MessageMapper messageMapper;
    private final OutboxService outboxService;

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put(ApplicationStatus.DELIVERED.name(),
                Set.of(ApplicationStatus.VIEWED.name(), ApplicationStatus.REJECTED.name()));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.VIEWED.name(),
                Set.of(ApplicationStatus.INTERVIEW_INVITED.name(), ApplicationStatus.REJECTED.name()));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.INTERVIEW_INVITED.name(),
                Set.of(ApplicationStatus.BOOKED.name(), ApplicationStatus.REJECTED.name()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationStatusVO createApplication(CreateApplicationRequest request) {
        Long userId = LoginUserContext.getUserId();
        String userType = LoginUserContext.get().getUserType();

        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有学生用户可以投递职位");
        }

        Job job = jobMapper.selectById(request.getJobId());
        if (job == null) {
            throw new BizException(ErrorCode.JOB_NOT_EXIST);
        }

        if (!JobStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BizException(ErrorCode.JOB_NOT_PUBLISHED);
        }

        Long resumeId = request.getResumeId();
        if (resumeId != null) {
            Resume resume = resumeMapper.selectById(resumeId);
            if (resume == null || !resume.getStudentId().equals(userId)) {
                throw new BizException(ErrorCode.RESUME_NOT_EXIST, "简历不存在或无权使用");
            }
        } else {
            LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Resume::getStudentId, userId)
                    .eq(Resume::getIsDefault, 1)
                    .last("LIMIT 1");
            Resume defaultResume = resumeMapper.selectOne(queryWrapper);
            if (defaultResume == null) {
                throw new BizException(ErrorCode.RESUME_NOT_EXIST, "请先创建并设置默认简历");
            }
            resumeId = defaultResume.getId();
        }

        LambdaQueryWrapper<JobApplication> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(JobApplication::getStudentId, userId)
                .eq(JobApplication::getJobId, request.getJobId());
        Long count = jobApplicationMapper.selectCount(checkWrapper);
        if (count > 0) {
            throw new BizException(ErrorCode.APPLICATION_DUPLICATE);
        }

        JobApplication application = new JobApplication();
        application.setStudentId(userId);
        application.setCompanyId(job.getCompanyId());
        application.setJobId(job.getId());
        application.setResumeId(resumeId);
        application.setStatus(ApplicationStatus.DELIVERED.name());
        application.setApplyTime(LocalDateTime.now());
        application.setCreateBy(userId);
        application.setUpdateBy(userId);
        jobApplicationMapper.insert(application);

        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(application.getId());
        log.setBeforeStatus(null);
        log.setAfterStatus(ApplicationStatus.DELIVERED.name());
        log.setOperatorId(userId);
        log.setOperatorType(UserType.STUDENT.name());
        log.setReason("投递职位");
        log.setCreateTime(LocalDateTime.now());
        applicationLogMapper.insert(log);

        jobMapper.incrementApplyCount(job.getId());

        sendDeliveryNotification(application, job);

        ApplicationStatusVO vo = new ApplicationStatusVO();
        vo.setApplicationId(application.getId());
        vo.setStatus(application.getStatus());
        vo.setApplyTime(application.getApplyTime());
        return vo;
    }

    @Override
    public PageResult<MyApplicationVO> getMyApplications(Integer pageNum, Integer pageSize, String status) {
        Long userId = LoginUserContext.getUserId();

        Page<JobApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(JobApplication::getStudentId, userId)
                .eq(status != null, JobApplication::getStatus, status)
                .orderByDesc(JobApplication::getApplyTime);
        IPage<JobApplication> applicationPage = jobApplicationMapper.selectPage(page, queryWrapper);

        List<JobApplication> records = applicationPage.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), applicationPage.getTotal(), pageNum, pageSize);
        }

        List<Long> jobIds = records.stream()
                .map(JobApplication::getJobId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> companyIds = records.stream()
                .map(JobApplication::getCompanyId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        Map<Long, CompanyProfile> companyMap = companyProfileMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(CompanyProfile::getId, c -> c));

        List<MyApplicationVO> voList = records.stream()
                .map(app -> {
                    MyApplicationVO vo = new MyApplicationVO();
                    vo.setId(app.getId());
                    vo.setJobId(app.getJobId());
                    vo.setResumeId(app.getResumeId());
                    vo.setStatus(app.getStatus());
                    vo.setApplyTime(app.getApplyTime());

                    Job job = jobMap.get(app.getJobId());
                    if (job != null) {
                        vo.setJobTitle(job.getTitle());
                        CompanyProfile company = companyMap.get(job.getCompanyId());
                        if (company != null) {
                            vo.setCompanyName(company.getCompanyName());
                        }
                    }
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(voList, applicationPage.getTotal(), pageNum, pageSize);
    }

    @Override
    public ApplicationDetailVO getMyApplicationDetail(Long applicationId) {
        Long userId = LoginUserContext.getUserId();

        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.APPLICATION_NOT_EXIST);
        }
        if (!userId.equals(application.getStudentId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该投递记录");
        }

        Job job = jobMapper.selectById(application.getJobId());
        CompanyProfile company = companyProfileMapper.selectById(application.getCompanyId());
        List<ApplicationLog> logs = applicationLogMapper.selectList(
                new LambdaQueryWrapper<ApplicationLog>()
                        .eq(ApplicationLog::getApplicationId, applicationId)
                        .orderByDesc(ApplicationLog::getCreateTime, ApplicationLog::getId));

        ApplicationDetailVO detailVO = new ApplicationDetailVO();
        detailVO.setId(application.getId());
        detailVO.setJobId(application.getJobId());
        detailVO.setCompanyId(application.getCompanyId());
        detailVO.setResumeId(application.getResumeId());
        detailVO.setStatus(application.getStatus());
        detailVO.setApplyTime(application.getApplyTime());

        if (job != null) {
            detailVO.setJobTitle(job.getTitle());
        }
        if (company != null) {
            detailVO.setCompanyName(company.getCompanyName());
        }

        List<ApplicationStatusLogVO> statusLogs = logs.stream().map(log -> {
            ApplicationStatusLogVO logVO = new ApplicationStatusLogVO();
            logVO.setBeforeStatus(log.getBeforeStatus());
            logVO.setAfterStatus(log.getAfterStatus());
            logVO.setOperatorType(log.getOperatorType());
            logVO.setReason(log.getReason());
            logVO.setCreateTime(log.getCreateTime());
            return logVO;
        }).collect(Collectors.toList());
        detailVO.setStatusLogs(statusLogs);

        return detailVO;
    }

    @Override
    public PageResult<ApplicationVO> getCompanyApplications(Integer pageNum, Integer pageSize, Long jobId, String status) {
        Long userId = LoginUserContext.getUserId();

        CompanyProfile companyProfile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (companyProfile == null) {
            throw new BizException(ErrorCode.COMPANY_UNVERIFIED, "企业资料不存在");
        }

        Long companyId = companyProfile.getId();

        Page<JobApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<JobApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(JobApplication::getCompanyId, companyId)
                .eq(jobId != null, JobApplication::getJobId, jobId)
                .eq(status != null, JobApplication::getStatus, status)
                .orderByDesc(JobApplication::getApplyTime);
        IPage<JobApplication> applicationPage = jobApplicationMapper.selectPage(page, queryWrapper);

        List<JobApplication> records = applicationPage.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(List.of(), applicationPage.getTotal(), pageNum, pageSize);
        }

        List<Long> studentIds = records.stream()
                .map(JobApplication::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> jobIds = records.stream()
                .map(JobApplication::getJobId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, StudentProfile> studentMap = studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>().in(StudentProfile::getUserId, studentIds))
                .stream().collect(Collectors.toMap(StudentProfile::getUserId, s -> s));

        Map<Long, Job> jobMap = jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        List<ApplicationVO> voList = records.stream()
                .map(app -> {
                    ApplicationVO vo = new ApplicationVO();
                    vo.setId(app.getId());
                    vo.setStudentId(app.getStudentId());
                    vo.setJobId(app.getJobId());
                    vo.setResumeId(app.getResumeId());
                    vo.setStatus(app.getStatus());
                    vo.setApplyTime(app.getApplyTime());

                    StudentProfile student = studentMap.get(app.getStudentId());
                    if (student != null) {
                        vo.setStudentName(student.getRealName());
                        vo.setSchool(student.getSchool());
                        vo.setMajor(student.getMajor());
                        vo.setEducation(student.getEducation());
                    }

                    Job job = jobMap.get(app.getJobId());
                    if (job != null) {
                        vo.setJobTitle(job.getTitle());
                        vo.setCompanyName(companyProfile.getCompanyName());
                    }
                    vo.setCompanyId(companyId);

                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(voList, applicationPage.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationStatusVO updateApplicationStatus(Long applicationId, UpdateApplicationStatusRequest request) {
        Long userId = LoginUserContext.getUserId();
        String userType = LoginUserContext.get().getUserType();

        if (!UserType.COMPANY.name().equals(userType)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有企业用户可以操作");
        }

        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BizException(ErrorCode.APPLICATION_NOT_EXIST);
        }

        CompanyProfile companyProfile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));
        if (companyProfile == null || !companyProfile.getId().equals(application.getCompanyId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该投递记录");
        }

        String currentStatus = application.getStatus();
        String targetStatus = request.getStatus();

        Set<String> allowedStatuses = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowedStatuses == null || !allowedStatuses.contains(targetStatus)) {
            throw new BizException(ErrorCode.APPLICATION_STATUS_INVALID,
                    "不允许的状态转换: " + currentStatus + " -> " + targetStatus);
        }

        String beforeStatus = application.getStatus();
        application.setStatus(targetStatus);
        application.setUpdateBy(userId);
        jobApplicationMapper.updateById(application);

        ApplicationLog log = new ApplicationLog();
        log.setApplicationId(application.getId());
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(targetStatus);
        log.setOperatorId(userId);
        log.setOperatorType(UserType.COMPANY.name());
        log.setReason(request.getReason());
        log.setCreateTime(LocalDateTime.now());
        applicationLogMapper.insert(log);

        sendStatusChangeNotification(application, beforeStatus, targetStatus, request.getReason());

        ApplicationStatusVO vo = new ApplicationStatusVO();
        vo.setApplicationId(application.getId());
        vo.setStatus(application.getStatus());
        vo.setApplyTime(application.getApplyTime());
        return vo;
    }

    private void sendDeliveryNotification(JobApplication application, Job job) {
        String messageId = java.util.UUID.randomUUID().toString();

        Message message = new Message();
        message.setMessageId(messageId);
        message.setReceiverId(application.getStudentId());
        message.setSenderId(application.getCompanyId());
        message.setMessageType("APPLICATION");
        message.setTitle("投递成功");
        message.setContent("您已成功投递 " + job.getTitle() + " 职位，请等待企业审核。");
        message.setBusinessType("APPLICATION");
        message.setBusinessId(application.getId());
        message.setReadStatus("UNREAD");
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        messageMapper.insert(message);

        Map<String, Object> mqMessage = Map.of(
                "messageId", messageId,
                "receiverId", application.getStudentId(),
                "senderId", application.getCompanyId(),
                "messageType", "APPLICATION",
                "title", "投递成功",
                "content", "您已成功投递 " + job.getTitle() + " 职位，请等待企业审核。",
                "businessType", "APPLICATION",
                "businessId", application.getId()
        );

        outboxService.sendAfterCommit(RabbitMQConstants.NOTIFY_EXCHANGE,
                RabbitMQConstants.NOTIFY_ROUTING_KEY, mqMessage,
                messageId, "NOTIFY", application.getId());
    }

    private void sendStatusChangeNotification(JobApplication application, String beforeStatus, String afterStatus, String reason) {
        String messageId = java.util.UUID.randomUUID().toString();

        String title = "投递状态更新";
        String content = "您的投递状态已从 " + beforeStatus + " 变更为 " + afterStatus;
        if (reason != null) {
            content += "，原因：" + reason;
        }

        Message message = new Message();
        message.setMessageId(messageId);
        message.setReceiverId(application.getStudentId());
        message.setSenderId(application.getCompanyId());
        message.setMessageType("APPLICATION");
        message.setTitle(title);
        message.setContent(content);
        message.setBusinessType("APPLICATION");
        message.setBusinessId(application.getId());
        message.setReadStatus("UNREAD");
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        messageMapper.insert(message);

        Map<String, Object> mqMessage = Map.of(
                "messageId", messageId,
                "receiverId", application.getStudentId(),
                "senderId", application.getCompanyId(),
                "messageType", "APPLICATION",
                "title", title,
                "content", content,
                "businessType", "APPLICATION",
                "businessId", application.getId()
        );

        outboxService.sendAfterCommit(RabbitMQConstants.NOTIFY_EXCHANGE,
                RabbitMQConstants.NOTIFY_ROUTING_KEY, mqMessage,
                messageId, "NOTIFY", application.getId());
    }
}
