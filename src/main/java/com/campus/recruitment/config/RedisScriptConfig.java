package com.campus.recruitment.config;

import com.campus.recruitment.common.constant.RedisConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<Long> interviewBookingScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/interview_booking.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
