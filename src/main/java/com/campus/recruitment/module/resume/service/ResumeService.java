package com.campus.recruitment.module.resume.service;

import com.campus.recruitment.entity.Resume;
import com.campus.recruitment.module.resume.dto.CreateResumeRequest;
import com.campus.recruitment.module.resume.vo.ResumeVO;

import java.util.List;

public interface ResumeService {

    ResumeVO createResume(CreateResumeRequest request);

    List<ResumeVO> getMyResumes();

    void setDefaultResume(Long resumeId);

    void deleteResume(Long resumeId);

    Resume getDefaultResume(Long studentId);

    Resume getResumeById(Long resumeId);
}
