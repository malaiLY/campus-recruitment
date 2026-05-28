package com.campus.recruitment.module.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.JobTag;
import com.campus.recruitment.mapper.CompanyProfileMapper;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.JobTagMapper;
import com.campus.recruitment.module.job.vo.JobVO;
import com.campus.recruitment.module.search.document.JobDocument;
import com.campus.recruitment.module.search.repository.JobSearchRepository;
import com.campus.recruitment.module.search.service.JobSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSearchServiceImpl implements JobSearchService {

    private final JobSearchRepository jobSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final JobMapper jobMapper;
    private final JobTagMapper jobTagMapper;
    private final CompanyProfileMapper companyProfileMapper;

    private static final String INDEX_NAME = "campus_job";

    @Override
    public PageResult<JobVO> search(String keyword, String city, Integer salaryMin, Integer salaryMax,
                                    String education, Integer pageNum, Integer pageSize) {
        try {
            Criteria criteria = new Criteria();

            if (keyword != null && !keyword.trim().isEmpty()) {
                Criteria keywordCriteria = new Criteria("title").matches(keyword)
                        .or(new Criteria("companyName").matches(keyword))
                        .or(new Criteria("description").matches(keyword))
                        .or(new Criteria("tags").matches(keyword));
                criteria = criteria.and(keywordCriteria);
            }

            if (city != null && !city.trim().isEmpty()) {
                criteria = criteria.and(new Criteria("city").is(city.trim()));
            }

            if (education != null && !education.trim().isEmpty()) {
                criteria = criteria.and(new Criteria("education").is(education.trim()));
            }

            if (salaryMin != null) {
                criteria = criteria.and(new Criteria("salaryMax").greaterThanEqual(salaryMin));
            }

            if (salaryMax != null) {
                criteria = criteria.and(new Criteria("salaryMin").lessThanEqual(salaryMax));
            }

            criteria = criteria.and(new Criteria("status").is("PUBLISHED"));

            Query query = new CriteriaQuery(criteria);
            query.setPageable(PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime")));

            SearchHits<JobDocument> searchHits = elasticsearchOperations.search(query, JobDocument.class,
                    IndexCoordinates.of(INDEX_NAME));

            List<JobVO> voList = searchHits.getSearchHits().stream()
                    .map(hit -> {
                        JobDocument doc = hit.getContent();
                        return convertToJobVO(doc);
                    })
                    .collect(Collectors.toList());

            return new PageResult<>(voList, searchHits.getTotalHits(), pageNum, pageSize);
        } catch (Exception e) {
            log.warn("ES search failed, fallback to MySQL query", e);
            return fallbackSearch(keyword, city, salaryMin, salaryMax, education, pageNum, pageSize);
        }
    }

    private PageResult<JobVO> fallbackSearch(String keyword, String city, Integer salaryMin, Integer salaryMax,
                                             String education, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Job::getStatus, "PUBLISHED");

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            queryWrapper.and(w -> w.like(Job::getTitle, kw)
                    .or().like(Job::getCategory, kw)
                    .or().like(Job::getCity, kw));
        }

        if (city != null && !city.trim().isEmpty()) {
            queryWrapper.eq(Job::getCity, city.trim());
        }

        if (salaryMin != null) {
            queryWrapper.ge(Job::getSalaryMax, salaryMin);
        }

        if (salaryMax != null) {
            queryWrapper.le(Job::getSalaryMin, salaryMax);
        }

        if (education != null && !education.trim().isEmpty()) {
            queryWrapper.eq(Job::getEducation, education.trim());
        }

        queryWrapper.orderByDesc(Job::getCreateTime);

        Page<Job> page = new Page<>(pageNum, pageSize);
        jobMapper.selectPage(page, queryWrapper);

        List<JobVO> voList = page.getRecords().stream().map(this::convertToJobVO).collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public void saveJob(Job job) {
        try {
            JobDocument doc = convertToJobDocument(job);
            jobSearchRepository.save(doc);
            log.info("Saved job to ES: jobId={}", job.getId());
        } catch (Exception e) {
            log.error("Failed to save job to ES: jobId={}", job.getId(), e);
        }
    }

    @Override
    public void deleteJob(Long jobId) {
        try {
            jobSearchRepository.deleteById(jobId);
            log.info("Deleted job from ES: jobId={}", jobId);
        } catch (Exception e) {
            log.error("Failed to delete job from ES: jobId={}", jobId, e);
        }
    }

    @Override
    public void syncJob(Job job) {
        try {
            Optional<JobDocument> existing = jobSearchRepository.findById(job.getId());
            if (existing.isPresent()) {
                JobDocument doc = convertToJobDocument(job);
                jobSearchRepository.save(doc);
                log.info("Synced job to ES: jobId={}", job.getId());
            } else {
                log.warn("Job not found in ES for sync: jobId={}", job.getId());
            }
        } catch (Exception e) {
            log.error("Failed to sync job in ES: jobId={}", job.getId(), e);
        }
    }

    @Override
    public boolean isIndexExists() {
        try {
            boolean exists = elasticsearchOperations.indexOps(JobDocument.class).exists();
            if (!exists) {
                elasticsearchOperations.indexOps(JobDocument.class).create();
                elasticsearchOperations.indexOps(JobDocument.class).putMapping();
                log.info("Created ES index: {}", INDEX_NAME);
            }
            return true;
        } catch (Exception e) {
            log.error("ES index check/creation failed", e);
            return false;
        }
    }

    private JobDocument convertToJobDocument(Job job) {
        JobDocument doc = new JobDocument();
        doc.setId(job.getId());
        doc.setCompanyId(job.getCompanyId());
        doc.setTitle(job.getTitle());
        doc.setCategory(job.getCategory());
        doc.setCity(job.getCity());
        doc.setSalaryMin(job.getSalaryMin());
        doc.setSalaryMax(job.getSalaryMax());
        doc.setEducation(job.getEducation());
        doc.setExperience(job.getExperience());
        doc.setDescription(job.getDescription());
        doc.setRequirement(job.getRequirement());
        doc.setStatus(job.getStatus());
        doc.setViewCount(job.getViewCount());
        doc.setApplyCount(job.getApplyCount());
        doc.setCreateTime(job.getCreateTime());

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getId, job.getCompanyId()));
        if (company != null) {
            doc.setCompanyName(company.getCompanyName());
        }

        List<JobTag> tags = jobTagMapper.selectList(
                new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId, job.getId()));
        if (tags != null && !tags.isEmpty()) {
            doc.setTags(tags.stream().map(JobTag::getTagName).collect(Collectors.toList()));
        }

        return doc;
    }

    private JobVO convertToJobVO(JobDocument doc) {
        JobVO vo = new JobVO();
        vo.setId(doc.getId());
        vo.setCompanyId(doc.getCompanyId());
        vo.setCompanyName(doc.getCompanyName());
        vo.setTitle(doc.getTitle());
        vo.setCategory(doc.getCategory());
        vo.setCity(doc.getCity());
        vo.setSalaryMin(doc.getSalaryMin());
        vo.setSalaryMax(doc.getSalaryMax());
        vo.setEducation(doc.getEducation());
        vo.setExperience(doc.getExperience());
        vo.setTags(doc.getTags());
        vo.setStatus(doc.getStatus());
        vo.setViewCount(doc.getViewCount());
        vo.setApplyCount(doc.getApplyCount());
        vo.setCreateTime(doc.getCreateTime());
        return vo;
    }

    private JobVO convertToJobVO(Job job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setCompanyId(job.getCompanyId());
        vo.setTitle(job.getTitle());
        vo.setCategory(job.getCategory());
        vo.setCity(job.getCity());
        vo.setSalaryMin(job.getSalaryMin());
        vo.setSalaryMax(job.getSalaryMax());
        vo.setEducation(job.getEducation());
        vo.setExperience(job.getExperience());
        vo.setStatus(job.getStatus());
        vo.setViewCount(job.getViewCount());
        vo.setApplyCount(job.getApplyCount());
        vo.setCreateTime(job.getCreateTime());

        CompanyProfile company = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getId, job.getCompanyId()));
        if (company != null) {
            vo.setCompanyName(company.getCompanyName());
        }

        List<JobTag> tags = jobTagMapper.selectList(
                new LambdaQueryWrapper<JobTag>().eq(JobTag::getJobId, job.getId()));
        if (tags != null && !tags.isEmpty()) {
            vo.setTags(tags.stream().map(JobTag::getTagName).collect(Collectors.toList()));
        }

        return vo;
    }
}
