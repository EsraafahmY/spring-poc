package com.poc.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Spring Configuration: type-safe binding of the "poc.security" block
 * (served by the Config Server) into a Java record.
 */
@ConfigurationProperties(prefix = "poc.security")
public record SecurityProperties(List<User> users) {
    public record User(String username, String password, List<String> roles) {}
}
