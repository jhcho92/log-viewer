package com.logviewer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application for testing the Log Viewer module.
 * In production, this module would be used as a dependency in another Spring Boot application.
 */
@SpringBootApplication
public class LogViewerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogViewerApplication.class, args);
        System.out.println("Log Viewer is running at http://localhost:8080/log-viewer");
    }
}
