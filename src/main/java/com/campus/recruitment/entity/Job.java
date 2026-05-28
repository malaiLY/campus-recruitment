package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    private String title;

    private String category;

    private String city;

    @TableField("salary_min")
    private Integer salaryMin;

    @TableField("salary_max")
    private Integer salaryMax;

    @TableField("salary_unit")
    private String salaryUnit;

    private String education;

    private String experience;

    private String description;

    private String requirement;

    private String status;

    @TableField("audit_reason")
    private String auditReason;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("apply_count")
    private Integer applyCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

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
