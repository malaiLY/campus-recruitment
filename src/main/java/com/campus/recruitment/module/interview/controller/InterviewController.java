package com.campus.recruitment.module.interview.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.interview.dto.BookInterviewRequest;
import com.campus.recruitment.module.interview.service.InterviewService;
import com.campus.recruitment.module.interview.vo.InterviewSlotVO;
import com.campus.recruitment.module.interview.vo.MyBookingVO;
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
@RequestMapping("/interview")
@RequiredArgsConstructor
@Validated
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/slots")
    public R<List<InterviewSlotVO>> getAvailableSlots(@RequestParam Long jobId) {
        return R.ok(interviewService.getAvailableSlots(jobId));
    }

    @RequireLogin
    @PostMapping("/bookings")
    public R<Void> bookInterview(@Valid @RequestBody BookInterviewRequest request) {
        interviewService.bookInterview(request);
        return R.ok();
    }

    @RequireLogin
    @GetMapping("/bookings/my")
    public R<List<MyBookingVO>> getMyBookings() {
        return R.ok(interviewService.getMyBookings());
    }

    @RequireLogin
    @PutMapping("/bookings/{id}/cancel")
    public R<Void> cancelBooking(@PathVariable Long id) {
        interviewService.cancelBooking(id);
        return R.ok();
    }
}
