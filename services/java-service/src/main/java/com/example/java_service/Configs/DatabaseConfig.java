package com.example.java_service.Configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.java_service.Repositories.Jpa")
@EnableMongoRepositories(basePackages = "com.example.java_service.Repositories.Mongodb")
public class DatabaseConfig {
}
