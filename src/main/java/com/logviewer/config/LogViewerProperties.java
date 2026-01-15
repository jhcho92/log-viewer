package com.logviewer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logviewer")
public class LogViewerProperties {

    /**
     * Enable or disable the log viewer module.
     */
    private boolean enabled = true;

    /**
     * Base path for the log viewer endpoints (e.g., "/log-viewer").
     */
    private String basePath = "/log-viewer";

    /**
     * Default log directory path. Can be changed at runtime via API.
     */
    private String defaultDirectory;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        // Ensure basePath starts with / and doesn't end with /
        if (basePath != null && !basePath.isEmpty()) {
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }
            if (basePath.endsWith("/")) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
        }
        this.basePath = basePath;
    }

    public String getDefaultDirectory() {
        return defaultDirectory;
    }

    public void setDefaultDirectory(String defaultDirectory) {
        this.defaultDirectory = defaultDirectory;
    }
}
