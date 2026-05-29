package com.campus.recruitment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.recruitment.entity.MqMessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface MqMessageLogMapper extends BaseMapper<MqMessageLog> {

    @Update("""
            UPDATE mq_message_log
            SET send_status = 'RETRYING',
                update_time = NOW(3)
            WHERE id = #{id}
              AND send_status IN ('SEND_FAILED', 'SENDING', 'RETRYING')
              AND update_time < #{threshold}
              AND send_retry_count < #{maxSendRetries}
            """)
    int claimRetry(@Param("id") Long id,
                   @Param("threshold") LocalDateTime threshold,
                   @Param("maxSendRetries") int maxSendRetries);
}
