package com.campus.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recruitment.entity.InterviewBooking;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterviewBookingMapper extends BaseMapper<InterviewBooking> {

    @Update("UPDATE interview_booking SET status = 'CANCELED', update_time = NOW(3) WHERE id = #{bookingId} AND student_id = #{studentId} AND status = 'BOOKED'")
    int cancelIfBooked(@Param("bookingId") Long bookingId, @Param("studentId") Long studentId);
}
