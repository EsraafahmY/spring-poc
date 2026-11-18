package com.poc.order.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Forwards the caller's Authorization + correlation id headers on service-to-service calls. */
@Configuration
public class FeignAuthForwardingConfig {

    @Bean
    RequestInterceptor authForwardingInterceptor() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String auth = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (auth != null) template.header(HttpHeaders.AUTHORIZATION, auth);
                String cid = attrs.getRequest().getHeader("X-Correlation-Id");
                if (cid != null) template.header("X-Correlation-Id", cid);
            }
        };
    }
}
