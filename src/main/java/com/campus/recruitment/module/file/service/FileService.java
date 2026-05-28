package com.campus.recruitment.module.file.service;

import com.campus.recruitment.module.file.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    FileUploadVO uploadFile(MultipartFile file, String bizType);

    String getFileDownloadUrl(Long fileId);
}
