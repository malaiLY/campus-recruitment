package com.campus.recruitment.module.admin.controller;

import com.campus.recruitment.common.annotation.OperationLog;
import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.annotation.RequirePermission;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.LoginLog;
import com.campus.recruitment.entity.SysUser;
import com.campus.recruitment.module.admin.dto.AuditRequest;
import com.campus.recruitment.module.admin.service.AdminService;
import com.campus.recruitment.module.admin.vo.DashboardVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
public class AdminController {

    private final AdminService adminService;

    @RequireLogin
    @RequirePermission("admin:dashboard")
    @GetMapping("/dashboard")
    public R<DashboardVO> getDashboard() {
        return R.ok(adminService.getDashboard());
    }

    @RequireLogin
    @RequirePermission("user:manage")
    @GetMapping("/users")
    public R<PageResult<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status) {
        return R.ok(adminService.listUsers(pageNum, pageSize, userType, status));
    }

    @RequireLogin
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", operation = "禁用用户")
    @PutMapping("/users/{id}/disable")
    public R<Void> disableUser(@PathVariable("id") Long id) {
        adminService.disableUser(id);
        return R.ok();
    }

    @RequireLogin
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", operation = "启用用户")
    @PutMapping("/users/{id}/enable")
    public R<Void> enableUser(@PathVariable("id") Long id) {
        adminService.enableUser(id);
        return R.ok();
    }

    @RequireLogin
    @RequirePermission("company:audit")
    @GetMapping("/companies/pending")
    public R<PageResult<CompanyProfile>> listPendingCompanyAudits(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.listPendingCompanyAudits(pageNum, pageSize));
    }

    @RequireLogin
    @RequirePermission("company:audit")
    @OperationLog(module = "企业审核", operation = "审核企业")
    @PutMapping("/companies/{id}/audit")
    public R<Void> auditCompany(
            @PathVariable("id") Long id,
            @Valid @RequestBody AuditRequest request) {
        Long auditorId = LoginUserContext.getUserId();
        adminService.auditCompany(id, request.getAuditStatus(), request.getReason(), auditorId);
        return R.ok();
    }

    @RequireLogin
    @RequirePermission("job:audit")
    @GetMapping("/jobs/pending")
    public R<PageResult<Job>> listPendingJobAudits(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.listPendingJobAudits(pageNum, pageSize));
    }

    @RequireLogin
    @RequirePermission("job:audit")
    @OperationLog(module = "岗位审核", operation = "审核岗位")
    @PutMapping("/jobs/{id}/audit")
    public R<Void> auditJob(
            @PathVariable("id") Long id,
            @Valid @RequestBody AuditRequest request) {
        adminService.auditJob(id, request.getAuditStatus(), request.getReason());
        return R.ok();
    }

    @RequireLogin
    @RequirePermission("log:view")
    @GetMapping("/logs/operation")
    public R<PageResult<com.campus.recruitment.entity.OperationLog>> listOperationLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long userId) {
        return R.ok(adminService.listOperationLogs(pageNum, pageSize, module, userId));
    }

    @RequireLogin
    @RequirePermission("log:view")
    @GetMapping("/logs/login")
    public R<PageResult<LoginLog>> listLoginLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username) {
        return R.ok(adminService.listLoginLogs(pageNum, pageSize, status, username));
    }
}
