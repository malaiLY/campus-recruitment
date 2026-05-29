package com.campus.recruitment.module.company.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.CompanyAuditStatus;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.entity.CompanyAudit;
import com.campus.recruitment.entity.CompanyProfile;
import com.campus.recruitment.entity.FileObject;
import com.campus.recruitment.mapper.CompanyAuditMapper;
import com.campus.recruitment.mapper.CompanyProfileMapper;
import com.campus.recruitment.mapper.FileObjectMapper;
import com.campus.recruitment.module.company.dto.CompanyCertificationDTO;
import com.campus.recruitment.module.company.dto.CompanyProfileDTO;
import com.campus.recruitment.module.company.service.CompanyService;
import com.campus.recruitment.module.company.vo.CertificationStatusVO;
import com.campus.recruitment.module.company.vo.CompanyProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyProfileMapper companyProfileMapper;
    private final CompanyAuditMapper companyAuditMapper;
    private final FileObjectMapper fileObjectMapper;

    @Override
    public CompanyProfileVO getProfile() {
        Long userId = LoginUserContext.getUserId();
        CompanyProfile profile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));

        if (profile == null) {
            return null;
        }

        CompanyProfileVO vo = new CompanyProfileVO();
        BeanUtils.copyProperties(profile, vo);
        return vo;
    }

    @Override
    public void saveProfile(CompanyProfileDTO dto) {
        Long userId = LoginUserContext.getUserId();

        if (dto.getLicenseFileId() != null) {
            validateLicenseFile(dto.getLicenseFileId(), userId);
        }

        CompanyProfile existing = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));

        if (existing != null) {
            if (CompanyAuditStatus.PENDING.name().equals(existing.getAuditStatus())) {
                throw new BizException(ErrorCode.COMPANY_AUDIT_PENDING);
            }
        }

        CompanyProfile profile;
        if (existing != null) {
            profile = existing;
        } else {
            profile = new CompanyProfile();
            profile.setUserId(userId);
            profile.setCreateTime(LocalDateTime.now());
            profile.setCreateBy(userId);
        }

        BeanUtils.copyProperties(dto, profile);
        profile.setUpdateTime(LocalDateTime.now());
        profile.setUpdateBy(userId);

        if (existing != null) {
            companyProfileMapper.updateById(profile);
        } else {
            companyProfileMapper.insert(profile);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitCertification(CompanyCertificationDTO dto) {
        if (dto.getLicenseFileId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "营业执照不能为空");
        }

        Long userId = LoginUserContext.getUserId();
        validateLicenseFile(dto.getLicenseFileId(), userId);
        CompanyProfile profile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));

        if (profile == null) {
            profile = new CompanyProfile();
            profile.setUserId(userId);
            profile.setCreateTime(LocalDateTime.now());
            profile.setCreateBy(userId);
        }

        BeanUtils.copyProperties(dto, profile);
        profile.setAuditStatus(CompanyAuditStatus.PENDING.name());
        profile.setUpdateTime(LocalDateTime.now());
        profile.setUpdateBy(userId);

        if (profile.getId() == null) {
            companyProfileMapper.insert(profile);
        } else {
            companyProfileMapper.updateById(profile);
        }

        CompanyAudit audit = new CompanyAudit();
        audit.setCompanyId(profile.getId());
        audit.setAuditStatus(CompanyAuditStatus.PENDING.name());
        audit.setAuditorId(null);
        audit.setAuditTime(null);
        audit.setCreateTime(LocalDateTime.now());
        companyAuditMapper.insert(audit);
    }

    private void validateLicenseFile(Long licenseFileId, Long userId) {
        FileObject file = fileObjectMapper.selectById(licenseFileId);
        if (file == null
                || !file.getOwnerId().equals(userId)
                || !"LICENSE".equals(file.getBizType())
                || !"NORMAL".equals(file.getStatus())) {
            throw new BizException(ErrorCode.FORBIDDEN, "营业执照文件无效");
        }
    }

    @Override
    public CertificationStatusVO getCertificationStatus() {
        Long userId = LoginUserContext.getUserId();
        CompanyProfile profile = companyProfileMapper.selectOne(
                new LambdaQueryWrapper<CompanyProfile>().eq(CompanyProfile::getUserId, userId));

        CertificationStatusVO vo = new CertificationStatusVO();
        if (profile != null) {
            vo.setAuditStatus(profile.getAuditStatus());
            vo.setAuditReason(profile.getAuditReason());
        } else {
            vo.setAuditStatus(CompanyAuditStatus.UNVERIFIED.name());
        }
        return vo;
    }
}
