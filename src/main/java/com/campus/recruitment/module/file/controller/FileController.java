package com.campus.recruitment.module.file.controller;

import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.module.file.service.FileService;
import com.campus.recruitment.module.file.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Validated
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @RequireLogin
    public R<FileUploadVO> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType) {
        FileUploadVO vo = fileService.uploadFile(file, bizType);
        return R.ok(vo);
    }

    @GetMapping("/{fileId}/download")
    @RequireLogin
    public R<String> getDownloadUrl(@PathVariable("fileId") Long fileId) {
        String url = fileService.getFileDownloadUrl(fileId);
        return R.ok(url);
    }
}
