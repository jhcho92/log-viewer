# Log Viewer

Spring Boot 애플리케이션에 플러그인 형태로 추가할 수 있는 실시간 로그 뷰어 라이브러리입니다.

## Features

- **플러그인 방식**: 다른 Spring Boot 프로젝트에 의존성 추가만으로 사용 가능
- **Auto-Configuration**: 별도 설정 없이 자동 활성화
- **실시간 스트리밍**: SSE(Server-Sent Events)를 통한 실시간 로그 모니터링
- **Live 토글**: 실시간 모드 ON/OFF 전환 가능
- **모던 UI**: macOS/iOS 스타일 Glassmorphism 디자인
- **로그 하이라이팅**: ERROR, WARN, INFO, DEBUG 레벨별 색상 구분
- **보안**: Path Traversal 방지, 권한 체크

---

## Quick Start

### 1. 라이브러리 빌드

```bash
./gradlew build
```

### 2. 프로젝트에 의존성 추가

```gradle
dependencies {
    implementation files('path/to/log-viewer-1.0.0.jar')
    // 또는 Maven Repository 사용 시:
    // implementation 'com.logviewer:log-viewer:1.0.0'
}
```

### 3. 브라우저에서 접속

```
http://localhost:8080/log-viewer
```

---

## UI Guide

### 화면 구성

```
+------------------+----------------------------------------+
|   SIDEBAR        |              MAIN VIEWER               |
+------------------+----------------------------------------+
| [Directory Input]|  [Traffic Lights]                      |
| [Load Button]    |  +----------------------------------+  |
|                  |  | filename.log    [*] Live [====] |  |
| +-------------+  |  +----------------------------------+  |
| | file1.log   |  |                                      |
| | file2.log   |  |  2024-01-13 10:00:00 INFO  Started  |
| | file3.txt   |  |  2024-01-13 10:00:01 DEBUG Request  |
| +-------------+  |  2024-01-13 10:00:02 ERROR Failed   |
|                  |                                      |
+------------------+----------------------------------------+
```

### 사용 방법

1. **디렉토리 설정**: 좌측 상단에 로그 디렉토리 경로 입력 후 `Load` 클릭
2. **파일 선택**: 파일 목록에서 원하는 로그 파일 클릭
3. **Live 모드**: 우측 상단 토글 스위치로 실시간 모드 ON/OFF
   - OFF (회색): 파일 내용 정적 표시
   - ON (녹색): 실시간 로그 스트리밍

### 로그 레벨 색상

| Level | Color | Keywords |
|-------|-------|----------|
| ERROR | 빨강 | error, exception, fail |
| WARN  | 노랑 | warn |
| INFO  | 청록 | info |
| DEBUG | 회색 | debug, trace |

---

## Configuration

### application.properties

```properties
# Log Viewer 활성화 (기본: true)
logviewer.enabled=true

# 기본 경로 (기본: /log-viewer)
logviewer.base-path=/log-viewer

# 기본 로그 디렉토리 (선택사항)
logviewer.default-directory=/var/log/myapp
```

### 설정 옵션 설명

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `logviewer.enabled` | boolean | `true` | 모듈 활성화 여부 |
| `logviewer.base-path` | String | `/log-viewer` | UI 및 API 기본 경로 |
| `logviewer.default-directory` | String | - | 시작 시 기본 로그 디렉토리 |

---

## API Reference

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `{basePath}` | 메인 HTML 페이지 |
| GET | `{basePath}/api/config` | 설정 정보 조회 |
| POST | `{basePath}/api/setDirectory` | 로그 디렉토리 설정 |
| GET | `{basePath}/api/files` | 로그 파일 목록 (.log, .txt) |
| GET | `{basePath}/api/content?file=` | 파일 내용 조회 |
| GET | `{basePath}/api/stream?file=` | 실시간 스트리밍 (SSE) |

### SSE Events

| Event | Description |
|-------|-------------|
| `init` | 초기 파일 전체 내용 |
| `update` | 새로 추가된 내용 |
| `reload` | 파일이 truncate된 경우 전체 재전송 |
| `error` | 오류 발생 |

### Error Types

| Error Type | Description |
|------------|-------------|
| `NOT_FOUND` | 경로가 존재하지 않음 |
| `NOT_DIRECTORY` | 디렉토리가 아님 |
| `NO_PERMISSION` | 읽기 권한 없음 |
| `ACCESS_DENIED` | 접근 거부 |
| `INVALID_PATH` | 잘못된 경로 (Path Traversal 시도) |
| `FILE_NOT_FOUND` | 파일을 찾을 수 없음 |
| `FILE_DELETED` | 파일이 삭제됨 |

---

## Project Structure

```
log-viewer/
├── build.gradle
├── settings.gradle
├── README.md
├── AI_RULES.md                    # AI 개발 가이드
└── src/main/
    ├── java/com/logviewer/
    │   ├── LogViewerApplication.java
    │   ├── config/
    │   │   ├── LogViewerAutoConfiguration.java
    │   │   ├── LogViewerProperties.java
    │   │   └── LogDirectoryConfig.java
    │   └── controller/
    │       └── LogViewerController.java
    └── resources/
        ├── META-INF/spring/
        │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
        ├── static/log-viewer/
        │   └── index.html
        └── application.properties
```

---

## Development

### 단독 실행 (테스트용)

```bash
# bootJar 빌드
./gradlew bootJar

# 실행
java -jar build/libs/log-viewer-1.0.0.jar
```

### 라이브러리 빌드

```bash
# JAR 빌드 (bootJar 제외)
./gradlew jar
```

---

## Tech Stack

- **Backend**: Java 17+, Spring Boot 3.x
- **Frontend**: Vanilla HTML/CSS/JavaScript
- **Streaming**: Server-Sent Events (SSE)
- **Design**: Glassmorphism, iOS-style UI

---

## Security

- **Path Traversal 방지**: 디렉토리 경계 체크
- **권한 검증**: 파일/디렉토리 읽기 권한 확인
- **입력 검증**: 경로 정규화 및 유효성 검사

---

## Troubleshooting

### 파일 목록이 표시되지 않음
- 디렉토리 경로가 올바른지 확인
- 해당 디렉토리에 `.log` 또는 `.txt` 파일이 있는지 확인
- 디렉토리 읽기 권한 확인

### 실시간 스트리밍이 작동하지 않음
- Live 토글이 ON 상태인지 확인
- 브라우저 개발자 도구에서 SSE 연결 상태 확인
- 서버 로그에서 오류 메시지 확인

### 한글이 깨짐
- 로그 파일이 UTF-8 인코딩인지 확인

---

## License

MIT License
