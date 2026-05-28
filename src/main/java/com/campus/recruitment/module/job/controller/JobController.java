package com.campus.recruitment.module.job.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.job.dto.JobQueryDTO;
import com.campus.recruitment.module.job.service.JobService;
import com.campus.recruitment.module.job.vo.JobDetailVO;
import com.campus.recruitment.module.job.vo.JobVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Validated
public class JobController {

    private final JobService jobService;

    @GetMapping
    public R<?> listJobs(@Valid JobQueryDTO queryDTO) {
        return R.ok(jobService.listJobs(queryDTO));
    }

    @GetMapping("/{id}")
    public R<JobDetailVO> getJobDetail(@PathVariable Long id) {
        return R.ok(jobService.getJobDetail(id));
    }

    @RequireLogin
    @PostMapping("/{id}/favorite")
    public R<Void> favoriteJob(@PathVariable Long id) {
        checkStudentUser();
        jobService.favoriteJob(id);
        return R.ok();
    }

    @RequireLogin
    @DeleteMapping("/{id}/favorite")
    public R<Void> unfavoriteJob(@PathVariable Long id) {
        checkStudentUser();
        jobService.unfavoriteJob(id);
        return R.ok();
    }

    private void checkStudentUser() {
        String userType = LoginUserContext.get().getUserType();
        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }
    }
}
