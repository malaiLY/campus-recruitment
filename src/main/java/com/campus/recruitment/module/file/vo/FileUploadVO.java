package com.campus.recruitment.module.file.vo;

import lombok.Data;

@Data
public class FileUploadVO {
    private Long fileId;
    private String originalName;
    private Long fileSize;
    private String fileExt;
    private String accessUrl;
}
