package com.campus.recruitment.module.application.service;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.module.application.dto.CreateApplicationRequest;
import com.campus.recruitment.module.application.dto.UpdateApplicationStatusRequest;
import com.campus.recruitment.module.application.vo.ApplicationDetailVO;
import com.campus.recruitment.module.application.vo.ApplicationStatusVO;
import com.campus.recruitment.module.application.vo.ApplicationVO;
import com.campus.recruitment.module.application.vo.MyApplicationVO;

public interface ApplicationService {

    ApplicationStatusVO createApplication(CreateApplicationRequest request);

    PageResult<MyApplicationVO> getMyApplications(Integer pageNum, Integer pageSize, String status);

    ApplicationDetailVO getMyApplicationDetail(Long applicationId);

    PageResult<ApplicationVO> getCompanyApplications(Integer pageNum, Integer pageSize, Long jobId, String status);

    ApplicationStatusVO updateApplicationStatus(Long applicationId, UpdateApplicationStatusRequest request);
}
