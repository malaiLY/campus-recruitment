package com.campus.recruitment.module.interview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookInterviewRequest {

    @NotNull
    private Long slotId;

    @NotNull
    private Long applicationId;
}
