package com.campus.recruitment.module.application.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.application.dto.CreateApplicationRequest;
import com.campus.recruitment.module.application.service.ApplicationService;
import com.campus.recruitment.module.application.vo.ApplicationDetailVO;
import com.campus.recruitment.module.application.vo.ApplicationStatusVO;
import com.campus.recruitment.module.application.vo.MyApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Validated
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @RequireLogin
    public R<ApplicationStatusVO> createApplication(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationStatusVO vo = applicationService.createApplication(request);
        return R.ok(vo);
    }

    @GetMapping("/my")
    @RequireLogin
    public R<PageResult<MyApplicationVO>> getMyApplications(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        PageResult<MyApplicationVO> page = applicationService.getMyApplications(pageNum, pageSize, status);
        return R.ok(page);
    }

    @GetMapping("/{id}")
    @RequireLogin
    public R<ApplicationDetailVO> getMyApplicationDetail(@PathVariable("id") Long id) {
        ApplicationDetailVO detail = applicationService.getMyApplicationDetail(id);
        return R.ok(detail);
    }
}
