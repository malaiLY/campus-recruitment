package com.campus.recruitment.module.dict.service;

import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.entity.SysDict;
import com.campus.recruitment.module.dict.dto.AdminDictCreateRequest;
import com.campus.recruitment.module.dict.dto.AdminDictUpdateRequest;

import java.util.List;

public interface DictService {

    List<SysDict> listEnabledByType(String dictType);

    PageResult<SysDict> listAdminDicts(Integer pageNum, Integer pageSize, String dictType, String status);

    SysDict createDict(AdminDictCreateRequest request);

    SysDict updateDict(Long id, AdminDictUpdateRequest request);

    void disableDict(Long id);
}
