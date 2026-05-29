package com.campus.recruitment.common.constant;

public class RabbitMQConstants {

    public static final String NOTIFY_EXCHANGE = "campus.notify.exchange";
    public static final String NOTIFY_QUEUE = "campus.notify.queue";
    public static final String NOTIFY_ROUTING_KEY = "campus.notify";

    public static final String NOTIFY_DLX_EXCHANGE = "campus.notify.dlx";
    public static final String NOTIFY_DLQ = "campus.notify.dlq";
    public static final String NOTIFY_DLX_ROUTING_KEY = "campus.notify.dlx";

    public static final String JOB_ES_EXCHANGE = "campus.job.exchange";
    public static final String JOB_ES_QUEUE = "campus.job.es.sync.queue";
    public static final String JOB_ES_ROUTING_KEY = "campus.job.es.sync";

    public static final String JOB_ES_DLX_EXCHANGE = "campus.job.es.dlx";
    public static final String JOB_ES_DLQ = "campus.job.es.dlq";
    public static final String JOB_ES_DLX_ROUTING_KEY = "campus.job.es.dlx";
}
