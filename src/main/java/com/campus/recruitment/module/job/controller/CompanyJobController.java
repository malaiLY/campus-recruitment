package com.campus.recruitment.module.job.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.job.dto.CreateJobRequest;
import com.campus.recruitment.module.job.dto.UpdateJobRequest;
import com.campus.recruitment.module.job.service.JobService;
import com.campus.recruitment.module.job.vo.JobVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company/jobs")
@RequiredArgsConstructor
@Validated
public class CompanyJobController {

    private final JobService jobService;

    @RequireLogin
    @GetMapping
    public R<PageResult<JobVO>> getMyJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(jobService.getMyJobs(status, pageNum, pageSize));
    }

    @RequireLogin
    @PostMapping
    public R<Long> createJob(@Valid @RequestBody CreateJobRequest request) {
        Long jobId = jobService.createJob(request);
        return R.ok(jobId);
    }

    @RequireLogin
    @PutMapping("/{id}")
    public R<Void> updateJob(@PathVariable Long id, @Valid @RequestBody UpdateJobRequest request) {
        jobService.updateJob(id, request);
        return R.ok();
    }

    @RequireLogin
    @PutMapping("/{id}/submit")
    public R<Void> submitForReview(@PathVariable Long id) {
        jobService.submitForReview(id);
        return R.ok();
    }

    @RequireLogin
    @PutMapping("/{id}/offline")
    public R<Void> offlineJob(@PathVariable Long id) {
        jobService.offlineJob(id);
        return R.ok();
    }
}
