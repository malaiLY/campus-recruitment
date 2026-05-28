package com.campus.recruitment.module.resume.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.common.enums.UserType;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.entity.FileObject;
import com.campus.recruitment.entity.Resume;
import com.campus.recruitment.mapper.FileObjectMapper;
import com.campus.recruitment.mapper.ResumeMapper;
import com.campus.recruitment.module.resume.dto.CreateResumeRequest;
import com.campus.recruitment.module.resume.service.ResumeService;
import com.campus.recruitment.module.resume.vo.ResumeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final FileObjectMapper fileObjectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO createResume(CreateResumeRequest request) {
        Long userId = LoginUserContext.getUserId();
        String userType = LoginUserContext.get().getUserType();

        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有学生用户可以创建简历");
        }

        FileObject fileObject = fileObjectMapper.selectById(request.getFileId());
        if (fileObject == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        if (!fileObject.getOwnerId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "文件不属于当前用户");
        }

        Resume resume = new Resume();
        resume.setStudentId(userId);
        resume.setResumeName(request.getResumeName());
        resume.setFileId(request.getFileId());
        resume.setIsDefault(request.isDefault() ? 1 : 0);
        resume.setStatus("NORMAL");
        resume.setCreateBy(userId);
        resume.setUpdateBy(userId);

        if (request.isDefault()) {
            LambdaUpdateWrapper<Resume> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Resume::getStudentId, userId)
                    .set(Resume::getIsDefault, 0);
            resumeMapper.update(null, updateWrapper);
        }

        resumeMapper.insert(resume);
        return buildResumeVO(resume, fileObject);
    }

    @Override
    public List<ResumeVO> getMyResumes() {
        Long userId = LoginUserContext.getUserId();

        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Resume::getStudentId, userId)
                .orderByDesc(Resume::getIsDefault)
                .orderByDesc(Resume::getCreateTime);
        List<Resume> resumes = resumeMapper.selectList(queryWrapper);

        if (resumes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> fileIds = resumes.stream()
                .map(Resume::getFileId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<FileObject> fileQueryWrapper = new LambdaQueryWrapper<>();
        fileQueryWrapper.in(FileObject::getId, fileIds);
        List<FileObject> fileObjects = fileObjectMapper.selectList(fileQueryWrapper);

        Map<Long, FileObject> fileMap = fileObjects.stream()
                .collect(Collectors.toMap(FileObject::getId, f -> f));

        return resumes.stream()
                .map(resume -> buildResumeVO(resume, fileMap.get(resume.getFileId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultResume(Long resumeId) {
        Long userId = LoginUserContext.getUserId();
        String userType = LoginUserContext.get().getUserType();

        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有学生用户可以设置默认简历");
        }

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简历不存在");
        }

        if (!resume.getStudentId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该简历");
        }

        LambdaUpdateWrapper<Resume> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Resume::getStudentId, userId)
                .set(Resume::getIsDefault, 0);
        resumeMapper.update(null, updateWrapper);

        LambdaUpdateWrapper<Resume> setDefaultWrapper = new LambdaUpdateWrapper<>();
        setDefaultWrapper.eq(Resume::getId, resumeId)
                .set(Resume::getIsDefault, 1);
        resumeMapper.update(null, setDefaultWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResume(Long resumeId) {
        Long userId = LoginUserContext.getUserId();
        String userType = LoginUserContext.get().getUserType();

        if (!UserType.STUDENT.name().equals(userType)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有学生用户可以删除简历");
        }

        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "简历不存在");
        }

        if (!resume.getStudentId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权删除该简历");
        }

        resumeMapper.deleteById(resumeId);
    }

    @Override
    public Resume getDefaultResume(Long studentId) {
        LambdaQueryWrapper<Resume> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Resume::getStudentId, studentId)
                .eq(Resume::getIsDefault, 1)
                .last("LIMIT 1");
        return resumeMapper.selectOne(queryWrapper);
    }

    @Override
    public Resume getResumeById(Long resumeId) {
        return resumeMapper.selectById(resumeId);
    }

    private ResumeVO buildResumeVO(Resume resume, FileObject fileObject) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setResumeName(resume.getResumeName());
        vo.setFileId(resume.getFileId());
        vo.setIsDefault(resume.getIsDefault());
        vo.setCreateTime(resume.getCreateTime());

        if (fileObject != null) {
            vo.setOriginalName(fileObject.getOriginalName());
            vo.setFileSize(fileObject.getFileSize());
        }

        return vo;
    }
}
