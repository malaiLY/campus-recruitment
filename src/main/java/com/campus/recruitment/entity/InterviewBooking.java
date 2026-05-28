package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_booking")
public class InterviewBooking {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("slot_id")
    private Long slotId;

    @TableField("application_id")
    private Long applicationId;

    @TableField("student_id")
    private Long studentId;

    @TableField("company_id")
    private Long companyId;

    @TableField("job_id")
    private Long jobId;

    private String status;

    @TableField("booking_time")
    private LocalDateTime bookingTime;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableLogic
    private Integer deleted;

    private String remark;
}
