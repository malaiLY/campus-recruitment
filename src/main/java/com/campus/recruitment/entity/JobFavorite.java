package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_favorite")
public class JobFavorite {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("job_id")
    private Long jobId;

    @TableField("favorite_time")
    private LocalDateTime favoriteTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
