package com.campus.recruitment.module.search.service;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.module.job.vo.JobVO;
import com.campus.recruitment.module.search.document.JobDocument;

public interface JobSearchService {

    PageResult<JobVO> search(String keyword, String city, Integer salaryMin, Integer salaryMax,
                             String education, Integer pageNum, Integer pageSize);

    void saveJob(Job job);

    void deleteJob(Long jobId);

    void syncJob(Job job);

    boolean isIndexExists();
}
