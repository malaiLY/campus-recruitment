package com.campus.recruitment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_object")
public class FileObject {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("owner_id")
    private Long ownerId;

    @TableField("biz_type")
    private String bizType;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("object_name")
    private String objectName;

    @TableField("original_name")
    private String originalName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("content_type")
    private String contentType;

    @TableField("file_ext")
    private String fileExt;

    @TableField("access_url")
    private String accessUrl;

    private String status;

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
