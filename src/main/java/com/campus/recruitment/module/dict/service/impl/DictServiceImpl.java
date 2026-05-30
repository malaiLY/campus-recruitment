package com.campus.recruitment.module.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.recruitment.common.exception.BizException;
import com.campus.recruitment.common.exception.ErrorCode;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.SysDict;
import com.campus.recruitment.mapper.SysDictMapper;
import com.campus.recruitment.module.dict.dto.AdminDictCreateRequest;
import com.campus.recruitment.module.dict.dto.AdminDictUpdateRequest;
import com.campus.recruitment.module.dict.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictMapper sysDictMapper;

    @Override
    public List<SysDict> listEnabledByType(String dictType) {
        return sysDictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getStatus, "NORMAL")
                .orderByAsc(SysDict::getSort, SysDict::getId));
    }

    @Override
    public PageResult<SysDict> listAdminDicts(Integer pageNum, Integer pageSize, String dictType, String status) {
        Page<SysDict> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<SysDict>()
                .eq(dictType != null && !dictType.trim().isEmpty(), SysDict::getDictType, dictType == null ? null : dictType.trim())
                .eq(status != null && !status.trim().isEmpty(), SysDict::getStatus, status == null ? null : status.trim())
                .orderByAsc(SysDict::getDictType, SysDict::getSort, SysDict::getId);
        sysDictMapper.selectPage(page, queryWrapper);
        return new PageResult<>(page.getRecords(), page.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDict createDict(AdminDictCreateRequest request) {
        validateUniqueDictPair(request.getDictType(), request.getDictKey(), null);

        SysDict dict = new SysDict();
        dict.setDictType(request.getDictType().trim());
        dict.setDictKey(request.getDictKey().trim());
        dict.setDictValue(request.getDictValue().trim());
        dict.setSort(request.getSort() == null ? 0 : request.getSort());
        dict.setStatus(request.getStatus() == null || request.getStatus().trim().isEmpty() ? "NORMAL" : request.getStatus().trim());
        dict.setRemark(request.getRemark());
        dict.setCreateTime(LocalDateTime.now());
        dict.setUpdateTime(LocalDateTime.now());
        sysDictMapper.insert(dict);
        return dict;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDict updateDict(Long id, AdminDictUpdateRequest request) {
        SysDict existed = sysDictMapper.selectById(id);
        if (existed == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "字典项不存在");
        }

        validateUniqueDictPair(request.getDictType(), request.getDictKey(), id);

        existed.setDictType(request.getDictType().trim());
        existed.setDictKey(request.getDictKey().trim());
        existed.setDictValue(request.getDictValue().trim());
        existed.setSort(request.getSort() == null ? 0 : request.getSort());
        existed.setStatus(request.getStatus() == null || request.getStatus().trim().isEmpty() ? "NORMAL" : request.getStatus().trim());
        existed.setRemark(request.getRemark());
        existed.setUpdateTime(LocalDateTime.now());
        sysDictMapper.updateById(existed);
        return existed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableDict(Long id) {
        SysDict existed = sysDictMapper.selectById(id);
        if (existed == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        if ("DISABLED".equals(existed.getStatus())) {
            return;
        }
        existed.setStatus("DISABLED");
        existed.setUpdateTime(LocalDateTime.now());
        sysDictMapper.updateById(existed);
    }

    private void validateUniqueDictPair(String dictType, String dictKey, Long excludeId) {
        LambdaQueryWrapper<SysDict> queryWrapper = new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType == null ? null : dictType.trim())
                .eq(SysDict::getDictKey, dictKey == null ? null : dictKey.trim());
        if (excludeId != null) {
            queryWrapper.ne(SysDict::getId, excludeId);
        }
        Long count = sysDictMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "字典类型和字典键已存在");
        }
    }
}
