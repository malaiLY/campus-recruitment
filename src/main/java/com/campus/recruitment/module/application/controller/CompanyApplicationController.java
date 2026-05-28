package com.campus.recruitment.module.application.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.application.dto.UpdateApplicationStatusRequest;
import com.campus.recruitment.module.application.service.ApplicationService;
import com.campus.recruitment.module.application.vo.ApplicationStatusVO;
import com.campus.recruitment.module.application.vo.ApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company/applications")
@RequiredArgsConstructor
@Validated
public class CompanyApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    @RequireLogin
    public R<PageResult<ApplicationVO>> getCompanyApplications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) String status) {
        PageResult<ApplicationVO> page = applicationService.getCompanyApplications(pageNum, pageSize, jobId, status);
        return R.ok(page);
    }

    @PutMapping("/{id}/status")
    @RequireLogin
    public R<ApplicationStatusVO> updateApplicationStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        ApplicationStatusVO vo = applicationService.updateApplicationStatus(id, request);
        return R.ok(vo);
    }
}
