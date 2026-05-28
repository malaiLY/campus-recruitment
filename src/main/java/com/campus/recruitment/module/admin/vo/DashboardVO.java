package com.campus.recruitment.module.admin.vo;

import lombok.Data;

@Data
public class DashboardVO {
    private Long userCount;
    private Long companyCount;
    private Long jobCount;
    private Long todayApplicationCount;
    private Long pendingCompanyAuditCount;
    private Long pendingJobAuditCount;
}
