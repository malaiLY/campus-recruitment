package com.campus.recruitment.common.constant;

public class RedisConstants {

    public static final String LOGIN_TOKEN_PREFIX = "campus:login:token:";

    public static final String USER_PERMISSION_PREFIX = "campus:user:permission:";

    public static final String JOB_DETAIL_PREFIX = "campus:job:detail:";

    public static final String JOB_HOT_ZSET = "campus:job:hot:zset";

    public static final String INTERVIEW_SLOT_STOCK_PREFIX = "campus:interview:slot:stock:";

    public static final String INTERVIEW_BOOKING_USER_PREFIX = "campus:interview:booking:user:";

    public static final String MESSAGE_UNREAD_PREFIX = "campus:message:unread:";

    public static final long LOGIN_TOKEN_TTL_SECONDS = 24 * 60 * 60;

    public static final long INTERVIEW_BOOKING_TTL_SECONDS = 24 * 60 * 60;
}
