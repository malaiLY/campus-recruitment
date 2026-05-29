package com.campus.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recruitment.entity.InterviewSlot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterviewSlotMapper extends BaseMapper<InterviewSlot> {

    @Update("UPDATE interview_slot SET remain_count = remain_count - 1, update_time = NOW(3) WHERE id = #{slotId} AND status = 'OPEN' AND remain_count > 0")
    int decrementRemainCount(@Param("slotId") Long slotId);

    @Update("UPDATE interview_slot SET remain_count = LEAST(remain_count + 1, capacity), update_time = NOW(3) WHERE id = #{slotId}")
    int incrementRemainCount(@Param("slotId") Long slotId);
}
