package com.campus.recruitment.module.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminDictUpdateRequest {

    @NotBlank(message = "dictType不能为空")
    private String dictType;

    @NotBlank(message = "dictKey不能为空")
    private String dictKey;

    @NotBlank(message = "dictValue不能为空")
    private String dictValue;

    private Integer sort;

    @Pattern(regexp = "NORMAL|DISABLED", message = "status仅支持NORMAL或DISABLED")
    private String status;

    private String remark;
}
