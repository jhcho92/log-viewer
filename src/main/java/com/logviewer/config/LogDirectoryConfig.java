package com.logviewer.config;

public class LogDirectoryConfig {

    private String logDirectory;

    public String getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    public boolean isConfigured() {
        return logDirectory != null && !logDirectory.isEmpty();
    }
}
