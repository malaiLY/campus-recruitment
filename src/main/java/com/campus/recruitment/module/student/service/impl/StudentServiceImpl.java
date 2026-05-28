package com.campus.recruitment.module.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.context.LoginUserContext;
import com.campus.recruitment.entity.StudentProfile;
import com.campus.recruitment.entity.StudentSkill;
import com.campus.recruitment.mapper.StudentProfileMapper;
import com.campus.recruitment.mapper.StudentSkillMapper;
import com.campus.recruitment.module.student.dto.StudentProfileDTO;
import com.campus.recruitment.module.student.service.StudentService;
import com.campus.recruitment.module.student.vo.StudentProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentProfileMapper studentProfileMapper;
    private final StudentSkillMapper studentSkillMapper;

    @Override
    public StudentProfileVO getProfile() {
        Long userId = LoginUserContext.getUserId();
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));

        if (profile == null) {
            return null;
        }

        StudentProfileVO vo = new StudentProfileVO();
        BeanUtils.copyProperties(profile, vo);

        List<StudentSkill> skills = studentSkillMapper.selectList(
                new LambdaQueryWrapper<StudentSkill>().eq(StudentSkill::getStudentId, profile.getId()));
        List<String> skillNames = new ArrayList<>();
        for (StudentSkill skill : skills) {
            skillNames.add(skill.getSkillName());
        }
        vo.setSkills(skillNames);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(StudentProfileDTO dto) {
        Long userId = LoginUserContext.getUserId();
        StudentProfile existing = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));

        StudentProfile profile;
        if (existing != null) {
            profile = existing;
        } else {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setCreateTime(LocalDateTime.now());
            profile.setCreateBy(userId);
        }

        BeanUtils.copyProperties(dto, profile);
        profile.setUpdateTime(LocalDateTime.now());
        profile.setUpdateBy(userId);

        if (existing != null) {
            studentProfileMapper.updateById(profile);
        } else {
            studentProfileMapper.insert(profile);
        }

        if (dto.getSkills() != null) {
            studentSkillMapper.delete(
                    new LambdaQueryWrapper<StudentSkill>().eq(StudentSkill::getStudentId, profile.getId()));

            LocalDateTime now = LocalDateTime.now();
            for (String skillName : dto.getSkills()) {
                StudentSkill skill = new StudentSkill();
                skill.setStudentId(profile.getId());
                skill.setSkillName(skillName);
                skill.setCreateTime(now);
                skill.setUpdateTime(now);
                studentSkillMapper.insert(skill);
            }
        }
    }
}
