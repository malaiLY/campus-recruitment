package com.campus.recruitment.module.job.service;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.module.job.dto.CreateJobRequest;
import com.campus.recruitment.module.job.dto.JobQueryDTO;
import com.campus.recruitment.module.job.dto.UpdateJobRequest;
import com.campus.recruitment.module.job.vo.JobDetailVO;
import com.campus.recruitment.module.job.vo.JobVO;

public interface JobService {

    PageResult<JobVO> listJobs(JobQueryDTO queryDTO);

    JobDetailVO getJobDetail(Long id);

    Long createJob(CreateJobRequest request);

    void updateJob(Long id, UpdateJobRequest request);

    void submitForReview(Long id);

    void offlineJob(Long id);

    void favoriteJob(Long jobId);

    void unfavoriteJob(Long jobId);

    PageResult<JobVO> getMyJobs(String status, Integer pageNum, Integer pageSize);
}
