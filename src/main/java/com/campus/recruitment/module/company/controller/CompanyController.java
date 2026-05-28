package com.campus.recruitment.module.company.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.company.dto.CompanyCertificationDTO;
import com.campus.recruitment.module.company.dto.CompanyProfileDTO;
import com.campus.recruitment.module.company.service.CompanyService;
import com.campus.recruitment.module.company.vo.CertificationStatusVO;
import com.campus.recruitment.module.company.vo.CompanyProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
@Validated
public class CompanyController {

    private final CompanyService companyService;

    @RequireLogin
    @GetMapping("/profile")
    public R<CompanyProfileVO> getProfile() {
        checkCompanyUser();
        return R.ok(companyService.getProfile());
    }

    @RequireLogin
    @PutMapping("/profile")
    public R<Void> saveProfile(@Valid @RequestBody CompanyProfileDTO dto) {
        checkCompanyUser();
        companyService.saveProfile(dto);
        return R.ok();
    }

    @RequireLogin
    @PostMapping("/certification")
    public R<Void> submitCertification(@Valid @RequestBody CompanyCertificationDTO dto) {
        checkCompanyUser();
        companyService.submitCertification(dto);
        return R.ok();
    }

    @RequireLogin
    @GetMapping("/certification/status")
    public R<CertificationStatusVO> getCertificationStatus() {
        checkCompanyUser();
        return R.ok(companyService.getCertificationStatus());
    }

    private void checkCompanyUser() {
        String userType = LoginUserContext.get().getUserType();
        if (!UserType.COMPANY.name().equals(userType)) {
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }
    }
}
