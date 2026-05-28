package com.campus.recruitment.common.constant;

public class RabbitMQConstants {

    public static final String NOTIFY_EXCHANGE = "campus.notify.exchange";
    public static final String NOTIFY_QUEUE = "campus.notify.queue";
    public static final String NOTIFY_ROUTING_KEY = "campus.notify";

    public static final String JOB_ES_EXCHANGE = "campus.job.exchange";
    public static final String JOB_ES_QUEUE = "campus.job.es.sync.queue";
    public static final String JOB_ES_ROUTING_KEY = "campus.job.es.sync";
}
