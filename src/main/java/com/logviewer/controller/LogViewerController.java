package com.logviewer.controller;

import com.logviewer.config.LogDirectoryConfig;
import com.logviewer.config.LogViewerProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class LogViewerController {

    private final LogDirectoryConfig config;
    private final LogViewerProperties properties;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LogViewerController(LogDirectoryConfig config, LogViewerProperties properties) {
        this.config = config;
        this.properties = properties;
    }

    /**
     * Serve the main HTML page
     */
    @GetMapping(value = "${logviewer.base-path:/log-viewer}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> index() {
        Resource resource = new ClassPathResource("static/log-viewer/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    /**
     * Get configuration info (basePath, etc.)
     */
    @GetMapping("${logviewer.base-path:/log-viewer}/api/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("basePath", properties.getBasePath());
        result.put("isConfigured", config.isConfigured());
        result.put("logDirectory", config.getLogDirectory());
        return ResponseEntity.ok(result);
    }

    /**
     * Set the log directory
     */
    @PostMapping("${logviewer.base-path:/log-viewer}/api/setDirectory")
    public ResponseEntity<Map<String, Object>> setDirectory(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();
        String path = request.get("path");

        if (path == null || path.trim().isEmpty()) {
            result.put("success", false);
            result.put("error", "경로를 입력해주세요.");
            result.put("errorType", "EMPTY_PATH");
            return ResponseEntity.badRequest().body(result);
        }

        path = path.trim();
        File directory = new File(path);

        if (!directory.exists()) {
            result.put("success", false);
            result.put("error", "해당 경로가 존재하지 않습니다: " + path);
            result.put("errorType", "NOT_FOUND");
            return ResponseEntity.badRequest().body(result);
        }

        if (!directory.isDirectory()) {
            result.put("success", false);
            result.put("error", "해당 경로는 디렉토리가 아닙니다: " + path);
            result.put("errorType", "NOT_DIRECTORY");
            return ResponseEntity.badRequest().body(result);
        }

        if (!directory.canRead()) {
            result.put("success", false);
            result.put("error", "해당 디렉토리에 대한 읽기 권한이 없습니다: " + path);
            result.put("errorType", "NO_PERMISSION");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            File[] files = directory.listFiles();
            if (files == null) {
                result.put("success", false);
                result.put("error", "디렉토리 내용을 읽을 수 없습니다: " + path);
                result.put("errorType", "ACCESS_DENIED");
                return ResponseEntity.badRequest().body(result);
            }
        } catch (SecurityException e) {
            result.put("success", false);
            result.put("error", "보안 제한으로 디렉토리에 접근할 수 없습니다: " + e.getMessage());
            result.put("errorType", "SECURITY_ERROR");
            return ResponseEntity.badRequest().body(result);
        }

        config.setLogDirectory(path);
        result.put("success", true);
        result.put("path", path);
        result.put("message", "로그 디렉토리가 설정되었습니다.");

        return ResponseEntity.ok(result);
    }

    /**
     * Get list of log files
     */
    @GetMapping("${logviewer.base-path:/log-viewer}/api/files")
    public ResponseEntity<Map<String, Object>> getFiles() {
        Map<String, Object> result = new HashMap<>();

        if (!config.isConfigured()) {
            result.put("success", false);
            result.put("error", "로그 디렉토리가 설정되지 않았습니다.");
            result.put("errorType", "NOT_CONFIGURED");
            result.put("files", Collections.emptyList());
            return ResponseEntity.ok(result);
        }

        try {
            List<Map<String, Object>> files = getLogFiles();
            result.put("success", true);
            result.put("files", files);
            result.put("directory", config.getLogDirectory());
        } catch (SecurityException e) {
            result.put("success", false);
            result.put("error", "디렉토리 접근 권한이 없습니다: " + e.getMessage());
            result.put("errorType", "SECURITY_ERROR");
            result.put("files", Collections.emptyList());
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "파일 목록을 가져오는 중 오류가 발생했습니다: " + e.getMessage());
            result.put("errorType", "UNKNOWN_ERROR");
            result.put("files", Collections.emptyList());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get file content
     */
    @GetMapping("${logviewer.base-path:/log-viewer}/api/content")
    public ResponseEntity<Map<String, Object>> getFileContent(@RequestParam String file) {
        Map<String, Object> result = new HashMap<>();

        if (!config.isConfigured()) {
            result.put("success", false);
            result.put("error", "로그 디렉토리가 설정되지 않았습니다.");
            result.put("errorType", "NOT_CONFIGURED");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            Path filePath = Paths.get(config.getLogDirectory(), file).normalize();
            Path basePath = Paths.get(config.getLogDirectory()).normalize();

            if (!filePath.startsWith(basePath)) {
                result.put("success", false);
                result.put("error", "잘못된 파일 경로입니다.");
                result.put("errorType", "INVALID_PATH");
                return ResponseEntity.badRequest().body(result);
            }

            File logFile = filePath.toFile();

            if (!logFile.exists()) {
                result.put("success", false);
                result.put("error", "파일을 찾을 수 없습니다: " + file);
                result.put("errorType", "FILE_NOT_FOUND");
                return ResponseEntity.badRequest().body(result);
            }

            if (!logFile.isFile()) {
                result.put("success", false);
                result.put("error", "해당 경로는 파일이 아닙니다: " + file);
                result.put("errorType", "NOT_A_FILE");
                return ResponseEntity.badRequest().body(result);
            }

            if (!logFile.canRead()) {
                result.put("success", false);
                result.put("error", "파일을 읽을 권한이 없습니다: " + file);
                result.put("errorType", "NO_PERMISSION");
                return ResponseEntity.badRequest().body(result);
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            result.put("success", true);
            result.put("content", content);
            result.put("fileName", file);
            result.put("size", logFile.length());
            result.put("lastModified", logFile.lastModified());

            return ResponseEntity.ok(result);

        } catch (AccessDeniedException e) {
            result.put("success", false);
            result.put("error", "파일 접근이 거부되었습니다: " + e.getMessage());
            result.put("errorType", "ACCESS_DENIED");
            return ResponseEntity.status(403).body(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("error", "파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
            result.put("errorType", "IO_ERROR");
            return ResponseEntity.status(500).body(result);
        } catch (SecurityException e) {
            result.put("success", false);
            result.put("error", "보안 제한으로 파일에 접근할 수 없습니다: " + e.getMessage());
            result.put("errorType", "SECURITY_ERROR");
            return ResponseEntity.status(403).body(result);
        }
    }

    /**
     * Stream log file updates via SSE
     */
    @GetMapping(value = "${logviewer.base-path:/log-viewer}/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLog(@RequestParam String file) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        if (!config.isConfigured()) {
            sendError(emitter, "로그 디렉토리가 설정되지 않았습니다.", "NOT_CONFIGURED");
            return emitter;
        }

        executor.execute(() -> {
            try {
                Path filePath = Paths.get(config.getLogDirectory(), file).normalize();
                Path basePath = Paths.get(config.getLogDirectory()).normalize();

                if (!filePath.startsWith(basePath)) {
                    sendError(emitter, "잘못된 파일 경로입니다.", "INVALID_PATH");
                    return;
                }

                File logFile = filePath.toFile();

                if (!logFile.exists() || !logFile.isFile()) {
                    sendError(emitter, "파일을 찾을 수 없습니다.", "FILE_NOT_FOUND");
                    return;
                }

                if (!logFile.canRead()) {
                    sendError(emitter, "파일을 읽을 권한이 없습니다.", "NO_PERMISSION");
                    return;
                }

                long lastPosition = logFile.length();
                long lastModified = logFile.lastModified();

                String initialContent = Files.readString(filePath, StandardCharsets.UTF_8);
                emitter.send(SseEmitter.event().name("init").data(initialContent));

                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(1000);

                    if (!logFile.exists()) {
                        sendError(emitter, "파일이 삭제되었습니다.", "FILE_DELETED");
                        return;
                    }

                    if (logFile.lastModified() > lastModified) {
                        long currentLength = logFile.length();

                        if (currentLength > lastPosition) {
                            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                                raf.seek(lastPosition);
                                byte[] bytes = new byte[(int) (currentLength - lastPosition)];
                                raf.readFully(bytes);
                                String newContent = new String(bytes, StandardCharsets.UTF_8);
                                emitter.send(SseEmitter.event().name("update").data(newContent));
                            }
                            lastPosition = currentLength;
                        } else if (currentLength < lastPosition) {
                            String content = Files.readString(filePath, StandardCharsets.UTF_8);
                            emitter.send(SseEmitter.event().name("reload").data(content));
                            lastPosition = currentLength;
                        }

                        lastModified = logFile.lastModified();
                    }
                }
            } catch (AccessDeniedException e) {
                sendError(emitter, "파일 접근이 거부되었습니다: " + e.getMessage(), "ACCESS_DENIED");
            } catch (IOException e) {
                sendError(emitter, "스트리밍 오류: " + e.getMessage(), "IO_ERROR");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        emitter.onCompletion(() -> {});
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> emitter.complete());

        return emitter;
    }

    private void sendError(SseEmitter emitter, String message, String errorType) {
        try {
            Map<String, String> errorData = new HashMap<>();
            errorData.put("message", message);
            errorData.put("type", errorType);
            emitter.send(SseEmitter.event().name("error").data(errorData));
            emitter.complete();
        } catch (IOException ignored) {
            emitter.completeWithError(new RuntimeException(message));
        }
    }

    private List<Map<String, Object>> getLogFiles() {
        List<Map<String, Object>> files = new ArrayList<>();

        if (!config.isConfigured()) {
            return files;
        }

        File directory = new File(config.getLogDirectory());
        File[] logFiles = directory.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".log") || lowerName.endsWith(".txt");
        });

        if (logFiles != null) {
            Arrays.sort(logFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

            for (File file : logFiles) {
                if (file.canRead()) {
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("size", file.length());
                    fileInfo.put("lastModified", file.lastModified());
                    fileInfo.put("readable", true);
                    files.add(fileInfo);
                }
            }
        }

        return files;
    }
}
