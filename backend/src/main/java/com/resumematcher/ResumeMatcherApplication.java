package com.resumematcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot application.
 *
 * @SpringBootApplication combines three annotations:
 *   @Configuration  – marks this class as a source of bean definitions
 *   @EnableAutoConfiguration – tells Spring Boot to guess config from classpath
 *   @ComponentScan  – scans com.resumematcher.* for @Component, @Service, etc.
 */
@SpringBootApplication
public class ResumeMatcherApplication {

    public static void main(String[] args) {
        // Boots up embedded Tomcat, creates the Spring context, and starts the app
        SpringApplication.run(ResumeMatcherApplication.class, args);
    }
}
