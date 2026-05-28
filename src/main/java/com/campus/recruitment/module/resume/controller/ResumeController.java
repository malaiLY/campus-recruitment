package com.campus.recruitment.module.resume.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.resume.dto.CreateResumeRequest;
import com.campus.recruitment.module.resume.service.ResumeService;
import com.campus.recruitment.module.resume.vo.ResumeVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resumes")
@RequiredArgsConstructor
@Validated
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    @RequireLogin
    public R<ResumeVO> createResume(@Valid @RequestBody CreateResumeRequest request) {
        ResumeVO vo = resumeService.createResume(request);
        return R.ok(vo);
    }

    @GetMapping("/my")
    @RequireLogin
    public R<List<ResumeVO>> getMyResumes() {
        List<ResumeVO> resumes = resumeService.getMyResumes();
        return R.ok(resumes);
    }

    @PutMapping("/{id}/default")
    @RequireLogin
    public R<Void> setDefaultResume(@PathVariable("id") Long id) {
        resumeService.setDefaultResume(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    public R<Void> deleteResume(@PathVariable("id") Long id) {
        resumeService.deleteResume(id);
        return R.ok();
    }
}
