package com.campus.recruitment.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recruitment.common.enums.CompanyAuditStatus;
import com.campus.recruitment.common.enums.JobStatus;
import com.campus.recruitment.common.enums.UserStatus;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.CompanyAudit;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.Job;
import com.campus.recruitment.entity.JobApplication;
import com.campus.recruitment.entity.LoginLog;
import com.campus.recruitment.entity.Message;
import com.campus.recruitment.entity.OperationLog;
import com.campus.recruitment.entity.SysUser;
import com.campus.recruitment.mapper.CompanyAuditMapper;
import com.campus.recruitment.mapper.CompanyProfileMapper;
import com.campus.recruitment.mapper.JobApplicationMapper;
import com.campus.recruitment.mapper.JobMapper;
import com.campus.recruitment.mapper.LoginLogMapper;
import com.campus.recruitment.mapper.MessageMapper;
import com.campus.recruitment.mapper.OperationLogMapper;
import com.campus.recruitment.mapper.SysUserMapper;
import com.campus.recruitment.module.admin.service.AdminService;
import com.campus.recruitment.module.admin.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper sysUserMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final CompanyAuditMapper companyAuditMapper;
    private final JobMapper jobMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final OperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final MessageMapper messageMapper;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();

        vo.setUserCount(sysUserMapper.selectCount(null));

        vo.setCompanyCount(companyProfileMapper.selectCount(null));

        vo.setJobCount(jobMapper.selectCount(null));

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setTodayApplicationCount(jobApplicationMapper.selectCount(
                new LambdaQueryWrapper<JobApplication>().ge(JobApplication::getApplyTime, todayStart)));

        vo.setPendingCompanyAuditCount(companyProfileMapper.selectCount(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getAuditStatus, CompanyAuditStatus.PENDING.name())));

        vo.setPendingJobAuditCount(jobMapper.selectCount(
                new LambdaQueryWrapper<Job>().eq(Job::getStatus, JobStatus.PENDING_REVIEW.name())));

        return vo;
    }

    @Override
    public PageResult<SysUser> listUsers(Integer pageNum, Integer pageSize, String userType, String status) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(userType != null && !userType.trim().isEmpty(), SysUser::getUserType, userType.trim())
                .eq(status != null && !status.trim().isEmpty(), SysUser::getStatus, status.trim())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        sysUserMapper.selectPage(page, queryWrapper);

        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        user.setStatus(UserStatus.DISABLED.name());
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.campus.recruitment.common.context.LoginUserContext.getUserId());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }

        user.setStatus(UserStatus.NORMAL.name());
        user.setUpdateTime(LocalDateTime.now());
        user.setUpdateBy(com.campus.recruitment.common.context.LoginUserContext.getUserId());
        sysUserMapper.updateById(user);
    }

    @Override
    public PageResult<CompanyProfile> listPendingCompanyAudits(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<CompanyProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CompanyProfile::getAuditStatus, CompanyAuditStatus.PENDING.name())
                .orderByAsc(CompanyProfile::getCreateTime);

        Page<CompanyProfile> page = new Page<>(pageNum, pageSize);
        companyProfileMapper.selectPage(page, queryWrapper);

        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditCompany(Long companyId, String auditStatus, String reason, Long auditorId) {
        CompanyProfile company = companyProfileMapper.selectById(companyId);
        if (company == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "企业资料不存在");
        }

        if (!CompanyAuditStatus.PENDING.name().equals(company.getAuditStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该企业审核状态不是待审核");
        }

        company.setAuditStatus(auditStatus);
        company.setAuditReason(reason);
        company.setUpdateTime(LocalDateTime.now());
        company.setUpdateBy(auditorId);
        companyProfileMapper.updateById(company);

        CompanyAudit audit = new CompanyAudit();
        audit.setCompanyId(companyId);
        audit.setAuditStatus(auditStatus);
        audit.setAuditReason(reason);
        audit.setAuditorId(auditorId);
        audit.setAuditTime(LocalDateTime.now());
        audit.setCreateTime(LocalDateTime.now());
        companyAuditMapper.insert(audit);

        sendAuditNotification(company.getUserId(), "企业认证审核" +
                (CompanyAuditStatus.APPROVED.name().equals(auditStatus) ? "通过" : "拒绝"),
                reason, "COMPANY_AUDIT", companyId);
    }

    @Override
    public PageResult<Job> listPendingJobAudits(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Job> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Job::getStatus, JobStatus.PENDING_REVIEW.name())
                .orderByAsc(Job::getCreateTime);

        Page<Job> page = new Page<>(pageNum, pageSize);
        jobMapper.selectPage(page, queryWrapper);

        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditJob(Long jobId, String auditStatus, String reason) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "岗位不存在");
        }

        if (!JobStatus.PENDING_REVIEW.name().equals(job.getStatus())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该岗位审核状态不是待审核");
        }

        if (CompanyAuditStatus.APPROVED.name().equals(auditStatus)) {
            job.setStatus(JobStatus.PUBLISHED.name());
            job.setAuditReason(reason);
        } else if (CompanyAuditStatus.REJECTED.name().equals(auditStatus)) {
            job.setStatus(JobStatus.REJECTED.name());
            job.setAuditReason(reason);
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, "审核状态不正确");
        }

        job.setUpdateTime(LocalDateTime.now());
        job.setUpdateBy(com.campus.recruitment.common.context.LoginUserContext.getUserId());
        jobMapper.updateById(job);

        if (CompanyAuditStatus.APPROVED.name().equals(auditStatus)) {
            syncJobToEs(job.getId());
        }

        CompanyProfile company = companyProfileMapper.selectById(job.getCompanyId());
        if (company != null) {
            sendAuditNotification(company.getUserId(), "岗位审核" +
                    (CompanyAuditStatus.APPROVED.name().equals(auditStatus) ? "通过" : "拒绝"),
                    "岗位 [" + job.getTitle() + "] 审核结果：" + reason, "JOB_AUDIT", jobId);
        }
    }

    @Override
    public PageResult<OperationLog> listOperationLogs(Integer pageNum, Integer pageSize, String module, Long userId) {
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(module != null && !module.trim().isEmpty(), OperationLog::getModule, module.trim())
                .eq(userId != null, OperationLog::getUserId, userId)
                .orderByDesc(OperationLog::getCreateTime);

        Page<OperationLog> page = new Page<>(pageNum, pageSize);
        operationLogMapper.selectPage(page, queryWrapper);

        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<LoginLog> listLoginLogs(Integer pageNum, Integer pageSize, String status, String username) {
        LambdaQueryWrapper<LoginLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(status != null && !status.trim().isEmpty(), LoginLog::getStatus, status.trim())
                .eq(username != null && !username.trim().isEmpty(), LoginLog::getUsername, username.trim())
                .orderByDesc(LoginLog::getLoginTime);

        Page<LoginLog> page = new Page<>(pageNum, pageSize);
        loginLogMapper.selectPage(page, queryWrapper);

        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void syncJobToEs(Long jobId) {
        try {
            String messageId = UUID.randomUUID().toString();
            Map<String, Object> message = Map.of(
                    "messageId", messageId,
                    "action", "save",
                    "jobId", jobId
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.JOB_ES_EXCHANGE,
                    RabbitMQConstants.JOB_ES_ROUTING_KEY,
                    message
            );
            log.info("发送岗位ES同步MQ消息: messageId={}, jobId={}, action=save", messageId, jobId);
        } catch (Exception e) {
            log.error("发送岗位ES同步MQ消息失败: {}", e.getMessage(), e);
        }
    }

    private void sendAuditNotification(Long userId, String title, String content, String businessType, Long businessId) {
        String messageId = UUID.randomUUID().toString();

        Message message = new Message();
        message.setMessageId(messageId);
        message.setReceiverId(userId);
        message.setSenderId(com.campus.recruitment.common.context.LoginUserContext.getUserId());
        message.setMessageType("AUDIT");
        message.setTitle(title);
        message.setContent(content);
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        message.setReadStatus(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        message.setDeleted(0);
        messageMapper.insert(message);

        Map<String, Object> mqMessage = Map.of(
                "messageId", messageId,
                "receiverId", userId,
                "senderId", com.campus.recruitment.common.context.LoginUserContext.getUserId(),
                "messageType", "AUDIT",
                "title", title,
                "content", content,
                "businessType", businessType,
                "businessId", businessId
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.NOTIFY_EXCHANGE,
                RabbitMQConstants.NOTIFY_ROUTING_KEY,
                mqMessage
        );
    }
}
