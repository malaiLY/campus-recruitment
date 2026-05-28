package com.campus.recruitment.module.interview.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.interview.dto.CreateInterviewSlotRequest;
import com.campus.recruitment.module.interview.service.InterviewService;
import com.campus.recruitment.module.interview.vo.InterviewBookingVO;
import com.campus.recruitment.module.interview.vo.InterviewSlotVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/company/interview")
@RequiredArgsConstructor
@Validated
public class CompanyInterviewController {

    private final InterviewService interviewService;

    @RequireLogin
    @PostMapping("/slots")
    public R<Long> createSlot(@Valid @RequestBody CreateInterviewSlotRequest request) {
        return R.ok(interviewService.createSlot(request));
    }

    @RequireLogin
    @GetMapping("/slots")
    public R<List<InterviewSlotVO>> getMySlots(@RequestParam(required = false) Long jobId) {
        return R.ok(interviewService.getCompanySlots(jobId));
    }

    @RequireLogin
    @PutMapping("/slots/{id}/close")
    public R<Void> closeSlot(@PathVariable Long id) {
        interviewService.closeSlot(id);
        return R.ok();
    }

    @RequireLogin
    @PutMapping("/slots/{id}/open")
    public R<Void> openSlot(@PathVariable Long id) {
        interviewService.openSlot(id);
        return R.ok();
    }

    @RequireLogin
    @GetMapping("/slots/{id}/bookings")
    public R<List<InterviewBookingVO>> getBookingsForSlot(@PathVariable Long id) {
        return R.ok(interviewService.getBookingsForSlot(id));
    }
}
