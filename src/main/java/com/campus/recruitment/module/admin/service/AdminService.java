package com.campus.recruitment.module.admin.service;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.LoginLog;
import com.campus.recruitment.entity.OperationLog;
import com.campus.recruitment.entity.SysUser;
import com.campus.recruitment.module.admin.vo.DashboardVO;

public interface AdminService {

    DashboardVO getDashboard();

    PageResult<SysUser> listUsers(Integer pageNum, Integer pageSize, String userType, String status);

    void disableUser(Long userId);

    void enableUser(Long userId);

    PageResult<CompanyProfile> listPendingCompanyAudits(Integer pageNum, Integer pageSize);

    void auditCompany(Long companyId, String auditStatus, String reason, Long auditorId);

    PageResult<Job> listPendingJobAudits(Integer pageNum, Integer pageSize);

    void auditJob(Long jobId, String auditStatus, String reason);

    PageResult<OperationLog> listOperationLogs(Integer pageNum, Integer pageSize, String module, Long userId);

    PageResult<LoginLog> listLoginLogs(Integer pageNum, Integer pageSize, String status, String username);
}
