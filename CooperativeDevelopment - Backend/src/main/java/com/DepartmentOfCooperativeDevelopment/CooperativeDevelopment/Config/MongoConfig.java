package com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = "com.DepartmentOfCooperativeDevelopment.CooperativeDevelopment.Repository",
        mongoTemplateRef = "primaryMongoTemplate"
)
public class MongoConfig {

    @Value("${mongodb.primary.uri}")
    private String primaryUri;

    @Value("${mongodb.secondary.uri}")
    private String secondaryUri;

    @Primary
    @Bean(name = "primaryMongoTemplate")
    public MongoTemplate primaryMongoTemplate() {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(primaryUri));
    }

    @Bean(name = "leaveMongoTemplate")
    public MongoTemplate leaveMongoTemplate() {
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(secondaryUri));
    }
}