package com.logviewer.config;

import com.logviewer.controller.LogViewerController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "logviewer", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LogViewerProperties.class)
public class LogViewerAutoConfiguration implements WebMvcConfigurer {

    private final LogViewerProperties properties;

    public LogViewerAutoConfiguration(LogViewerProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public LogDirectoryConfig logDirectoryConfig() {
        LogDirectoryConfig config = new LogDirectoryConfig();
        if (properties.getDefaultDirectory() != null && !properties.getDefaultDirectory().isEmpty()) {
            config.setLogDirectory(properties.getDefaultDirectory());
        }
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public LogViewerController logViewerController(LogDirectoryConfig logDirectoryConfig) {
        return new LogViewerController(logDirectoryConfig, properties);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = properties.getBasePath();

        // Serve static resources from classpath:/static/log-viewer/
        registry.addResourceHandler(basePath + "/static/**")
                .addResourceLocations("classpath:/static/log-viewer/");
    }
}
