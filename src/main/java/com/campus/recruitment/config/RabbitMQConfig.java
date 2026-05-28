package com.campus.recruitment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.campus.recruitment.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    @Bean
    public TopicExchange notifyExchange() {
        return new TopicExchange(RabbitMQConstants.NOTIFY_EXCHANGE);
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(RabbitMQConstants.NOTIFY_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.NOTIFY_EXCHANGE + ".dlx")
                .withArgument("x-dead-letter-routing-key", "campus.notify.dlx")
                .build();
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue())
                .to(notifyExchange())
                .with(RabbitMQConstants.NOTIFY_ROUTING_KEY);
    }

    @Bean
    public TopicExchange jobEsExchange() {
        return new TopicExchange(RabbitMQConstants.JOB_ES_EXCHANGE);
    }

    @Bean
    public Queue jobEsQueue() {
        return QueueBuilder.durable(RabbitMQConstants.JOB_ES_QUEUE).build();
    }

    @Bean
    public Binding jobEsBinding() {
        return BindingBuilder.bind(jobEsQueue())
                .to(jobEsExchange())
                .with(RabbitMQConstants.JOB_ES_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
        return template;
    }
}
