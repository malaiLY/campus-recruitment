package com.campus.recruitment.module.student.service;

import com.campus.recruitment.module.student.dto.StudentProfileDTO;
import com.campus.recruitment.module.student.vo.StudentProfileVO;

public interface StudentService {

    StudentProfileVO getProfile();

    void saveProfile(StudentProfileDTO dto);
}
