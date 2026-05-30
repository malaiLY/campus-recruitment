package com.campus.recruitment.module.dict.controller;

import com.campus.recruitment.common.result.R;
import com.campus.recruitment.entity.SysDict;
import com.campus.recruitment.module.dict.service.DictService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
@Validated
public class DictController {

    private final DictService dictService;

    @GetMapping
    public R<List<SysDict>> listByType(@RequestParam @NotBlank(message = "dictType不能为空") String dictType) {
        return R.ok(dictService.listEnabledByType(dictType));
    }
}
