package com.campus.recruitment.module.dict.controller;

import com.campus.recruitment.common.annotation.OperationLog;
import com.campus.recruitment.common.annotation.RequireLogin;
import com.campus.recruitment.common.annotation.RequirePermission;
import com.campus.recruitment.common.result.PageResult;
import com.campus.recruitment.common.result.R;
import com.campus.recruitment.entity.SysDict;
import com.campus.recruitment.module.dict.dto.AdminDictCreateRequest;
import com.campus.recruitment.module.dict.dto.AdminDictUpdateRequest;
import com.campus.recruitment.module.dict.service.DictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dicts")
@RequiredArgsConstructor
@Validated
public class AdminDictController {

    private final DictService dictService;

    @RequireLogin
    @RequirePermission("user:manage")
    @GetMapping
    public R<PageResult<SysDict>> listDicts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String status) {
        return R.ok(dictService.listAdminDicts(pageNum, pageSize, dictType, status));
    }

    @RequireLogin
    @RequirePermission("user:manage")
    @OperationLog(module = "字典管理", operation = "新增字典项")
    @PostMapping
    public R<SysDict> createDict(@Valid @RequestBody AdminDictCreateRequest request) {
        return R.ok(dictService.createDict(request));
    }

    @RequireLogin
    @RequirePermission("user:manage")
    @OperationLog(module = "字典管理", operation = "修改字典项")
    @PutMapping("/{id}")
    public R<SysDict> updateDict(@PathVariable("id") Long id, @Valid @RequestBody AdminDictUpdateRequest request) {
        return R.ok(dictService.updateDict(id, request));
    }

    @RequireLogin
    @RequirePermission("user:manage")
    @OperationLog(module = "字典管理", operation = "停用字典项")
    @DeleteMapping("/{id}")
    public R<Void> disableDict(@PathVariable("id") Long id) {
        dictService.disableDict(id);
        return R.ok();
    }
}
