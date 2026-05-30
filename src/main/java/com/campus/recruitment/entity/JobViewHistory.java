package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_view_history")
public class JobViewHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("job_id")
    private Long jobId;

    @TableField("view_ip")
    private String viewIp;

    @TableField("view_time")
    private LocalDateTime viewTime;

    @TableField("create_time")
    private LocalDateTime createTime;
}
