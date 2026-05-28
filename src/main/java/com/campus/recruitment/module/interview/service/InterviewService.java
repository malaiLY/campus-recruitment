package com.campus.recruitment.module.interview.service;

import com.campus.recruitment.module.interview.dto.BookInterviewRequest;
import com.campus.recruitment.module.interview.dto.CreateInterviewSlotRequest;
import com.campus.recruitment.module.interview.vo.InterviewBookingVO;
import com.campus.recruitment.module.interview.vo.InterviewSlotVO;
import com.campus.recruitment.module.interview.vo.MyBookingVO;

import java.util.List;

public interface InterviewService {

    Long createSlot(CreateInterviewSlotRequest request);

    List<InterviewSlotVO> getCompanySlots(Long jobId);

    void closeSlot(Long slotId);

    void openSlot(Long slotId);

    List<InterviewBookingVO> getBookingsForSlot(Long slotId);

    List<InterviewSlotVO> getAvailableSlots(Long jobId);

    void bookInterview(BookInterviewRequest request);

    void cancelBooking(Long bookingId);

    List<MyBookingVO> getMyBookings();
}
