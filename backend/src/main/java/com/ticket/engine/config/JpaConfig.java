package com.ticket.engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.ticket.engine.repository")
@EntityScan(basePackages = "com.ticket.engine.entity")
public class JpaConfig {
}
