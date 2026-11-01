package com.hrms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Configuration
@EnableMongoAuditing(dateTimeProviderRef = "utcDateTimeProvider")
@EnableTransactionManagement
public class MongoConfig {

    @Bean
    DateTimeProvider utcDateTimeProvider(Clock utcClock) {
        return () -> Optional.of(Instant.now(utcClock));
    }

    @Bean
    MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
