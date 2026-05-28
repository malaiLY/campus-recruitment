package com.campus.recruitment.common.result;

import com.campus.recruitment.common.exception.BizException;
import lombok.Data;

@Data
public class R<T> {

    private int code;
    private String message;
    private T data;

    public R() {}

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(0, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(0, "success", data);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(500, message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(BizException e) {
        return new R<>(e.getCode(), e.getMessage(), null);
    }
}
