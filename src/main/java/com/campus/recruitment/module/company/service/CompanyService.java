package com.campus.recruitment.module.company.service;

import com.campus.recruitment.module.company.dto.CompanyCertificationDTO;
import com.campus.recruitment.module.company.dto.CompanyProfileDTO;
import com.campus.recruitment.module.company.vo.CertificationStatusVO;
import com.campus.recruitment.module.company.vo.CompanyProfileVO;

public interface CompanyService {

    CompanyProfileVO getProfile();

    void saveProfile(CompanyProfileDTO dto);

    void submitCertification(CompanyCertificationDTO dto);

    CertificationStatusVO getCertificationStatus();
}
