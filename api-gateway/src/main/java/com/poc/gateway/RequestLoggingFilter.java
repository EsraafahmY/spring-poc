package com.poc.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Global filter: adds a correlation id to every request and logs it. Registered as a bean via @Component (IoC). */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        String correlationId = id != null ? id : UUID.randomUUID().toString();
        log.info("{} {} correlationId={}", exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(), correlationId);
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID, correlationId)).build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
