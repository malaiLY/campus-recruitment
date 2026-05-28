package com.campus.recruitment.module.student.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.student.dto.StudentProfileDTO;
import com.campus.recruitment.module.student.service.StudentService;
import com.campus.recruitment.module.student.vo.StudentProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
@Validated
public class StudentController {

    private final StudentService studentService;

    @RequireLogin
    @GetMapping("/profile")
    public R<StudentProfileVO> getProfile() {
        checkStudentUser();
        return R.ok(studentService.getProfile());
    }

    @RequireLogin
    @PutMapping("/profile")
    public R<Void> saveProfile(@Valid @RequestBody StudentProfileDTO dto) {
        checkStudentUser();
        studentService.saveProfile(dto);
        return R.ok();
    }

    private void checkStudentUser() {
        String userType = LoginUserContext.get().getUserType();
        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.USER_TYPE_MISMATCH);
        }
    }
}
