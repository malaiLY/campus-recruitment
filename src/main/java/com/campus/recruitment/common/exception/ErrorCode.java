package com.campus.recruitment.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统异常"),

    USER_EXIST(10001, "用户名已存在"),
    PASSWORD_ERROR(10002, "密码错误"),
    USER_DISABLED(10003, "账号被禁用"),
    USER_TYPE_MISMATCH(10004, "用户类型不匹配"),

    COMPANY_UNVERIFIED(20001, "企业未认证"),
    COMPANY_AUDIT_PENDING(20002, "企业认证待审核"),

    RESUME_NOT_EXIST(30001, "未上传简历"),
    APPLICATION_DUPLICATE(30002, "重复投递"),
    JOB_NOT_EXIST(30003, "岗位不存在"),
    JOB_NOT_PUBLISHED(30004, "岗位未发布"),
    JOB_STATUS_INVALID(30005, "岗位状态不允许操作"),
    APPLICATION_STATUS_INVALID(30006, "投递状态不允许操作"),
    APPLICATION_NOT_EXIST(30007, "投递记录不存在"),

    INTERVIEW_NO_INVITE(40001, "未获得面试邀约"),
    INTERVIEW_DUPLICATE(40002, "重复预约"),
    INTERVIEW_FULL(40003, "名额已满"),
    INTERVIEW_EXPIRED(40004, "场次已过期"),
    INTERVIEW_SLOT_NOT_EXIST(40005, "面试场次不存在"),

    FILE_TYPE_NOT_ALLOWED(50001, "文件类型不支持"),
    FILE_TOO_LARGE(50002, "文件过大"),

    MQ_CONSUME_FAIL(60001, "消息消费失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
