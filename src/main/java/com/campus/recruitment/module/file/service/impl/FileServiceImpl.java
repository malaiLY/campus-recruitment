package com.campus.recruitment.module.file.service.impl;

import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.FileBizType;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.entity.FileObject;
import com.campus.recruitment.mapper.FileObjectMapper;
import com.campus.recruitment.module.file.service.FileService;
import com.campus.recruitment.module.file.vo.FileUploadVO;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final FileObjectMapper fileObjectMapper;

    @Value("${app.minio.bucket-resume}")
    private String bucketResume;

    @Value("${app.minio.bucket-avatar}")
    private String bucketAvatar;

    @Value("${app.minio.bucket-license}")
    private String bucketLicense;

    private static final long MAX_RESUME_SIZE = 10 * 1024 * 1024;
    private static final long MAX_OTHER_SIZE = 5 * 1024 * 1024;

    private static final Set<String> RESUME_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> AVATAR_EXTENSIONS = Set.of("jpg", "png", "jpeg");
    private static final Set<String> LICENSE_EXTENSIONS = Set.of("jpg", "png", "jpeg", "pdf");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO uploadFile(MultipartFile file, String bizType) {
        validateFile(file, bizType);
        validateFileMagic(file, getFileExtension(file.getOriginalFilename()));

        String fileExt = getFileExtension(file.getOriginalFilename());
        String objectName = generateObjectName(bizType, fileExt);
        String bucketName = getBucketName(bizType);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }

        FileObject fileObject = new FileObject();
        fileObject.setOwnerId(LoginUserContext.getUserId());
        fileObject.setBizType(bizType);
        fileObject.setBucketName(bucketName);
        fileObject.setObjectName(objectName);
        fileObject.setOriginalName(file.getOriginalFilename());
        fileObject.setFileSize(file.getSize());
        fileObject.setContentType(file.getContentType());
        fileObject.setFileExt(fileExt);
        fileObject.setStatus("NORMAL");
        fileObject.setCreateBy(LoginUserContext.getUserId());
        fileObject.setUpdateBy(LoginUserContext.getUserId());
        fileObjectMapper.insert(fileObject);

        FileUploadVO vo = new FileUploadVO();
        vo.setFileId(fileObject.getId());
        vo.setOriginalName(fileObject.getOriginalName());
        vo.setFileSize(fileObject.getFileSize());
        vo.setFileExt(fileObject.getFileExt());
        vo.setAccessUrl(getAccessUrl(fileObject.getId()));
        return vo;
    }

    @Override
    public String getFileDownloadUrl(Long fileId) {
        FileObject fileObject = fileObjectMapper.selectById(fileId);
        if (fileObject == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        Long userId = LoginUserContext.getUserId();
        String userType = LoginUserContext.get().getUserType();

        if (!fileObject.getOwnerId().equals(userId) && !UserType.ADMIN.name().equals(userType)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权限访问该文件");
        }

        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(fileObject.getBucketName())
                    .object(fileObject.getObjectName())
                    .expiry(60 * 60)
                    .build()
            );
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "生成下载链接失败");
        }
    }

    private void validateFile(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        long maxSize = "RESUME".equals(bizType) ? MAX_RESUME_SIZE : MAX_OTHER_SIZE;
        if (file.getSize() > maxSize) {
            String maxMb = String.valueOf(maxSize / (1024 * 1024));
            throw new BizException(ErrorCode.FILE_TOO_LARGE, "文件过大，最大支持" + maxMb + "MB");
        }

        String fileExt = getFileExtension(file.getOriginalFilename());
        Set<String> allowedExtensions = getAllowedExtensions(bizType);
        if (!allowedExtensions.contains(fileExt.toLowerCase())) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "不支持的文件类型: " + fileExt);
        }

        String contentType = file.getContentType();
        if (contentType == null || !isContentTypeConsistent(contentType, fileExt)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
        }
    }

    private boolean isContentTypeConsistent(String contentType, String fileExt) {
        return switch (fileExt) {
            case "pdf" -> contentType.equals("application/pdf");
            case "doc" -> contentType.equals("application/msword");
            case "docx" -> contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "jpg", "jpeg" -> contentType.equals("image/jpeg");
            case "png" -> contentType.equals("image/png");
            default -> true;
        };
    }

    private void validateFileMagic(MultipartFile file, String fileExt) {
        try (InputStream is = file.getInputStream(); BufferedInputStream bis = new BufferedInputStream(is)) {
            byte[] header = new byte[8];
            int read = bis.read(header);
            if (read < 4) {
                throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件头读取失败");
            }

            boolean valid = switch (fileExt) {
                case "pdf" -> header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46; // %PDF
                case "jpg", "jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8; // FF D8
                case "png" -> (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47; // .PNG
                case "doc" -> header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF && header[2] == 0x11 && header[3] == (byte) 0xE0; // OLE
                case "docx" -> header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04; // ZIP (docx is zip)
                default -> true;
            };

            if (!valid) {
                throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与声明的格式不匹配");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "文件校验失败");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件名无效");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private Set<String> getAllowedExtensions(String bizType) {
        FileBizType bizTypeEnum = FileBizType.valueOf(bizType);
        return switch (bizTypeEnum) {
            case RESUME -> RESUME_EXTENSIONS;
            case AVATAR -> AVATAR_EXTENSIONS;
            case LICENSE -> LICENSE_EXTENSIONS;
            default -> Set.of("pdf", "doc", "docx", "jpg", "png", "jpeg");
        };
    }

    private String generateObjectName(String bizType, String fileExt) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return bizType + "/" + datePath + "/" + uuid + "." + fileExt;
    }

    private String getBucketName(String bizType) {
        FileBizType bizTypeEnum = FileBizType.valueOf(bizType);
        return switch (bizTypeEnum) {
            case RESUME -> bucketResume;
            case AVATAR -> bucketAvatar;
            case LICENSE -> bucketLicense;
            default -> bucketResume;
        };
    }

    private String getAccessUrl(Long fileId) {
        return "/api/files/" + fileId + "/download";
    }
}
