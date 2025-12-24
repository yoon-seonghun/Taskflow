# 파일 스토리지 시스템 설계 계획서

| 항목 | 내용 |
|------|------|
| 문서 버전 | 1.0 |
| 작성일 | 2024-12-23 |
| 대상 버전 | TaskFlow v1.1 |
| 상태 | 승인 대기 |

---

## 1. 개요

### 1.1 목적
마크다운 에디터에서 이미지 첨부 기능을 지원하기 위한 파일 업로드/관리 시스템 구축

### 1.2 범위
- 파일 업로드, 조회, 삭제 기능
- 마크다운 에디터 이미지 삽입 UI
- 스토리지 추상화를 통한 확장성 확보

### 1.3 목표
| 목표 | 설명 |
|------|------|
| 유연성 | 로컬 → NAS → S3 마이그레이션 지원 |
| 확장성 | 신규 스토리지 타입 추가 용이 |
| 사용성 | 드래그앤드롭, 클립보드 붙여넣기 지원 |
| 보안성 | 파일 타입 검증, 접근 권한 관리 |

---

## 2. 시스템 아키텍처

### 2.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Frontend                                    │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     MarkdownEditor.vue                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │   │
│  │  │ 툴바 버튼   │  │ Drag&Drop  │  │ Clipboard Paste        │  │   │
│  │  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘  │   │
│  │         └────────────────┴─────────────────────┘                 │   │
│  │                          │                                       │   │
│  │                    fileApi.upload()                              │   │
│  └──────────────────────────┼───────────────────────────────────────┘   │
└─────────────────────────────┼───────────────────────────────────────────┘
                              │ HTTP POST /api/files/upload
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                              Backend                                     │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     FileController                               │   │
│  │                          │                                       │   │
│  │                     FileService                                  │   │
│  │                          │                                       │   │
│  │         ┌────────────────┴────────────────┐                     │   │
│  │         │                                 │                     │   │
│  │    FileMapper                   FileStorageService              │   │
│  │    (메타데이터)                    (파일 저장)                    │   │
│  │         │                                 │                     │   │
│  └─────────┼─────────────────────────────────┼─────────────────────┘   │
└────────────┼─────────────────────────────────┼─────────────────────────┘
             │                                 │
             ▼                                 ▼
┌────────────────────┐           ┌────────────────────────────────────────┐
│      MySQL         │           │           File Storage                 │
│  ┌──────────────┐  │           │  ┌──────────┐ ┌────────┐ ┌─────────┐  │
│  │   TB_FILE    │  │           │  │  LOCAL   │ │  NAS   │ │   S3    │  │
│  │  (메타데이터) │  │           │  │ (Docker) │ │ (SMB)  │ │ (AWS)   │  │
│  └──────────────┘  │           │  └──────────┘ └────────┘ └─────────┘  │
└────────────────────┘           └────────────────────────────────────────┘
```

### 2.2 스토리지 추상화 계층

```
┌─────────────────────────────────────────────────────────────┐
│                  FileStorageService                          │
│                     <<interface>>                            │
├─────────────────────────────────────────────────────────────┤
│  + upload(file, directory): String                          │
│  + download(storagePath): Resource                          │
│  + delete(storagePath): boolean                             │
│  + exists(storagePath): boolean                             │
│  + getAccessUrl(storagePath): String                        │
│  + getStorageType(): StorageType                            │
└─────────────────────────────────────────────────────────────┘
                              △
                              │ implements
          ┌───────────────────┼───────────────────┐
          │                   │                   │
┌─────────┴─────────┐ ┌───────┴───────┐ ┌────────┴────────┐
│ LocalFileStorage  │ │ NasFileStorage│ │  S3FileStorage  │
├───────────────────┤ ├───────────────┤ ├─────────────────┤
│ basePath:         │ │ basePath:     │ │ endpoint:       │
│ /data/uploads     │ │ /mnt/nas      │ │ s3.aws.com      │
│                   │ │               │ │ bucket: files   │
│ Phase 1 구현      │ │ Phase 3 구현  │ │ Phase 3 구현    │
└───────────────────┘ └───────────────┘ └─────────────────┘
```

---

## 3. 데이터베이스 설계

### 3.1 TB_FILE 테이블

#### ERD
```
┌─────────────────────────────────────────────────────────────┐
│                         TB_FILE                              │
├─────────────────────────────────────────────────────────────┤
│ PK  FILE_ID          BIGINT         AUTO_INCREMENT          │
├─────────────────────────────────────────────────────────────┤
│     ORIGINAL_NAME    VARCHAR(255)   원본 파일명              │
│     STORED_NAME      VARCHAR(255)   저장 파일명 (UUID)       │
│     EXTENSION        VARCHAR(20)    파일 확장자              │
│     MIME_TYPE        VARCHAR(100)   MIME 타입               │
│     FILE_SIZE        BIGINT         파일 크기 (bytes)        │
├─────────────────────────────────────────────────────────────┤
│     STORAGE_TYPE     VARCHAR(20)    스토리지 타입            │
│     STORAGE_PATH     VARCHAR(500)   저장 경로 (상대 경로)     │
│     STORAGE_BUCKET   VARCHAR(100)   버킷/공유명              │
├─────────────────────────────────────────────────────────────┤
│     RELATED_TYPE     VARCHAR(50)    연관 엔티티 타입          │
│ FK  RELATED_ID       BIGINT         연관 엔티티 ID           │
├─────────────────────────────────────────────────────────────┤
│     USE_YN           CHAR(1)        사용 여부 (Y/N)          │
│     CREATED_AT       DATETIME       생성일시                 │
│ FK  CREATED_BY       BIGINT         생성자 ID                │
│     UPDATED_AT       DATETIME       수정일시                 │
│ FK  UPDATED_BY       BIGINT         수정자 ID                │
└─────────────────────────────────────────────────────────────┘

인덱스:
  - IDX_FILE_RELATED: (RELATED_TYPE, RELATED_ID)
  - IDX_FILE_STORAGE: (STORAGE_TYPE, STORAGE_PATH)
  - IDX_FILE_CREATED: (CREATED_BY, CREATED_AT)
```

#### DDL
```sql
CREATE TABLE TB_FILE (
    FILE_ID         BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '파일 ID',

    -- 원본 파일 정보
    ORIGINAL_NAME   VARCHAR(255)    NOT NULL COMMENT '원본 파일명',
    STORED_NAME     VARCHAR(255)    NOT NULL COMMENT '저장된 파일명 (UUID)',
    EXTENSION       VARCHAR(20)     NULL COMMENT '파일 확장자',
    MIME_TYPE       VARCHAR(100)    NULL COMMENT 'MIME 타입',
    FILE_SIZE       BIGINT          NOT NULL DEFAULT 0 COMMENT '파일 크기 (bytes)',

    -- 스토리지 정보
    STORAGE_TYPE    VARCHAR(20)     NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL, NAS, S3',
    STORAGE_PATH    VARCHAR(500)    NOT NULL COMMENT '스토리지 내 상대 경로',
    STORAGE_BUCKET  VARCHAR(100)    NULL COMMENT 'S3 버킷명 또는 NAS 공유명',

    -- 연관 정보
    RELATED_TYPE    VARCHAR(50)     NULL COMMENT 'ITEM, COMMENT, USER 등',
    RELATED_ID      BIGINT          NULL COMMENT '연관 엔티티 ID',

    -- 상태
    USE_YN          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용 여부',

    -- 공통
    CREATED_AT      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY      BIGINT          NOT NULL,
    UPDATED_AT      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY      BIGINT          NULL,

    INDEX IDX_FILE_RELATED (RELATED_TYPE, RELATED_ID),
    INDEX IDX_FILE_STORAGE (STORAGE_TYPE, STORAGE_PATH(100)),
    INDEX IDX_FILE_CREATED (CREATED_BY, CREATED_AT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='파일 메타데이터';
```

### 3.2 스토리지 타입 정의

| STORAGE_TYPE | 설명 | STORAGE_PATH 형식 | STORAGE_BUCKET |
|--------------|------|-------------------|----------------|
| LOCAL | Docker 볼륨/로컬 | `2024/12/uuid.png` | NULL |
| NAS | 네트워크 스토리지 | `2024/12/uuid.png` | `share_name` |
| S3 | AWS S3 / MinIO | `2024/12/uuid.png` | `bucket_name` |

### 3.3 연관 타입 정의

| RELATED_TYPE | 설명 | 용도 |
|--------------|------|------|
| ITEM | 업무 아이템 | 업무 설명 내 이미지 |
| COMMENT | 댓글 | 댓글 내 이미지 |
| USER | 사용자 | 프로필 이미지 (추후) |
| BOARD | 보드 | 보드 커버 이미지 (추후) |

---

## 4. API 설계

### 4.1 API 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/files/upload` | 파일 업로드 | 필수 |
| GET | `/api/files/{fileId}` | 파일 다운로드/표시 | 필수 |
| GET | `/api/files/{fileId}/info` | 파일 정보 조회 | 필수 |
| DELETE | `/api/files/{fileId}` | 파일 삭제 | 필수 |
| GET | `/api/files/related/{type}/{id}` | 연관 파일 목록 | 필수 |

### 4.2 파일 업로드

```
POST /api/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

Request Parameters:
┌─────────────┬──────────┬──────────┬───────────────────────────┐
│ Parameter   │ Type     │ Required │ Description               │
├─────────────┼──────────┼──────────┼───────────────────────────┤
│ file        │ File     │ O        │ 업로드할 파일              │
│ relatedType │ String   │ X        │ ITEM, COMMENT 등          │
│ relatedId   │ Long     │ X        │ 연관 엔티티 ID             │
└─────────────┴──────────┴──────────┴───────────────────────────┘

Response (200 OK):
{
  "success": true,
  "data": {
    "file_id": 1,
    "original_name": "screenshot.png",
    "stored_name": "a1b2c3d4-e5f6-7890-abcd-ef1234567890.png",
    "extension": "png",
    "mime_type": "image/png",
    "file_size": 102400,
    "storage_type": "LOCAL",
    "url": "/api/files/1",
    "markdown": "![screenshot.png](/api/files/1)",
    "created_at": "2024-12-23T10:30:00"
  },
  "message": "파일이 업로드되었습니다"
}

Error Responses:
- 400: 파일이 비어있거나 유효하지 않음
- 413: 파일 크기 초과 (최대 10MB)
- 415: 지원하지 않는 파일 형식
```

### 4.3 파일 다운로드/표시

```
GET /api/files/{fileId}
Authorization: Bearer {token}

Path Parameters:
┌───────────┬──────────┬───────────────────────────┐
│ Parameter │ Type     │ Description               │
├───────────┼──────────┼───────────────────────────┤
│ fileId    │ Long     │ 파일 ID                   │
└───────────┴──────────┴───────────────────────────┘

Response Headers:
- Content-Type: {mime_type}
- Content-Disposition: inline; filename="{original_name}"
- Content-Length: {file_size}
- Cache-Control: public, max-age=31536000

Response Body: Binary file content

Error Responses:
- 404: 파일을 찾을 수 없음
- 403: 접근 권한 없음
```

### 4.4 파일 정보 조회

```
GET /api/files/{fileId}/info
Authorization: Bearer {token}

Response (200 OK):
{
  "success": true,
  "data": {
    "file_id": 1,
    "original_name": "screenshot.png",
    "stored_name": "a1b2c3d4.png",
    "extension": "png",
    "mime_type": "image/png",
    "file_size": 102400,
    "file_size_display": "100 KB",
    "storage_type": "LOCAL",
    "related_type": "ITEM",
    "related_id": 5,
    "url": "/api/files/1",
    "created_at": "2024-12-23T10:30:00",
    "created_by": 1,
    "created_by_name": "관리자"
  }
}
```

### 4.5 파일 삭제

```
DELETE /api/files/{fileId}
Authorization: Bearer {token}

Response (200 OK):
{
  "success": true,
  "message": "파일이 삭제되었습니다"
}

Error Responses:
- 404: 파일을 찾을 수 없음
- 403: 삭제 권한 없음 (본인 또는 관리자만 삭제 가능)
```

---

## 5. 프론트엔드 설계

### 5.1 MarkdownEditor 이미지 업로드 UI

```
┌─────────────────────────────────────────────────────────────────────────┐
│ [편집] [미리보기]                                                        │
│                                    [B][I][~][H1][H2][📷][📎][`][```][>] │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  # 업무 설명                                                            │
│                                                                         │
│  이 업무는 다음과 같은 작업을 포함합니다.                                  │
│                                                                         │
│  ![스크린샷](/api/files/1)                                              │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  📷 이미지를 여기에 드래그하거나 붙여넣기 하세요 (Ctrl+V)         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│  ⬆️ 업로드 중... screenshot.png (45%)  ████████░░░░░░░░░░░░             │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 이미지 업로드 방식

| 방식 | 트리거 | 동작 |
|------|--------|------|
| 툴바 버튼 | 📷 버튼 클릭 | 파일 선택 다이얼로그 표시 |
| 드래그 앤 드롭 | 에디터에 파일 드롭 | 즉시 업로드 시작 |
| 클립보드 | Ctrl+V | 클립보드 이미지 업로드 |

### 5.3 업로드 플로우

```
사용자 액션 (파일 선택/드롭/붙여넣기)
         │
         ▼
┌─────────────────┐
│ 파일 유효성 검사 │
│ - 크기 확인     │
│ - 타입 확인     │
└────────┬────────┘
         │ 유효
         ▼
┌─────────────────┐
│ 업로드 진행률   │
│ 표시 시작       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ POST /api/files │
│ /upload         │
└────────┬────────┘
         │ 성공
         ▼
┌─────────────────┐
│ 마크다운 삽입   │
│ ![name](url)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 진행률 표시     │
│ 숨김            │
└─────────────────┘
```

### 5.4 컴포넌트 구조

```
MarkdownEditor.vue
├── 상태
│   ├── isUploading: boolean
│   ├── uploadProgress: number
│   └── uploadFileName: string
│
├── 메서드
│   ├── handleImageUpload(file: File)
│   ├── handleDrop(event: DragEvent)
│   ├── handlePaste(event: ClipboardEvent)
│   └── insertImageMarkdown(url: string, name: string)
│
└── 이벤트
    ├── @drop.prevent → handleDrop
    ├── @paste → handlePaste
    └── 📷 버튼 @click → openFileDialog
```

---

## 6. 설정 및 환경

### 6.1 application.yml

```yaml
# 파일 스토리지 설정
storage:
  # 활성 스토리지 타입: LOCAL, NAS, S3
  type: LOCAL

  # 공통 설정
  max-file-size: 10MB
  allowed-extensions:
    - jpg
    - jpeg
    - png
    - gif
    - webp
    - svg
    - pdf
    - doc
    - docx
    - xls
    - xlsx
    - ppt
    - pptx
    - txt
    - zip

  # 로컬 스토리지
  local:
    base-path: /data/uploads
    url-prefix: /api/files

  # NAS 스토리지 (추후)
  nas:
    base-path: /mnt/nas/taskflow
    share-name: taskflow_files
    url-prefix: /api/files

  # S3 스토리지 (추후)
  s3:
    endpoint: ${S3_ENDPOINT:https://s3.amazonaws.com}
    bucket: ${S3_BUCKET:taskflow-files}
    region: ${S3_REGION:ap-northeast-2}
    access-key: ${S3_ACCESS_KEY:}
    secret-key: ${S3_SECRET_KEY:}
    url-prefix: ${S3_URL_PREFIX:}
```

### 6.2 Docker Compose 볼륨

```yaml
version: '3.8'

services:
  backend:
    # ...
    volumes:
      - upload_data:/data/uploads
    environment:
      - STORAGE_TYPE=LOCAL
      - STORAGE_LOCAL_BASE_PATH=/data/uploads

volumes:
  upload_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data/uploads  # 호스트 경로 (선택적)
```

---

## 7. 보안 설계

### 7.1 파일 업로드 보안

| 항목 | 대책 |
|------|------|
| 파일 타입 검증 | 확장자 + MIME 타입 이중 검증 |
| 파일 크기 제한 | 기본 10MB, 설정 가능 |
| 파일명 처리 | UUID로 저장, 원본명은 DB에만 보관 |
| 경로 조작 방지 | 상대 경로 검증, `..` 차단 |
| 실행 파일 차단 | .exe, .sh, .bat 등 차단 |

### 7.2 파일 접근 권한

| 작업 | 권한 |
|------|------|
| 업로드 | 로그인 사용자 |
| 조회 | 로그인 사용자 (추후 공유 설정에 따라 제한 가능) |
| 삭제 | 업로더 본인 또는 관리자 |

### 7.3 악성 파일 대응

```java
// 이미지 파일 검증 예시
public boolean isValidImage(MultipartFile file) {
    // 1. 확장자 검사
    String extension = getExtension(file.getOriginalFilename());
    if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
        return false;
    }

    // 2. MIME 타입 검사
    String mimeType = file.getContentType();
    if (!mimeType.startsWith("image/")) {
        return false;
    }

    // 3. 매직 바이트 검사 (파일 시그니처)
    byte[] header = new byte[8];
    file.getInputStream().read(header);
    return isValidImageSignature(header);
}
```

---

## 8. 마이그레이션 전략

### 8.1 LOCAL → NAS 마이그레이션

```
┌─────────────────────────────────────────────────────────────┐
│                    마이그레이션 절차                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1: 파일 동기화                                        │
│  ┌─────────────┐         rsync          ┌─────────────┐    │
│  │   LOCAL     │ ─────────────────────▶ │    NAS      │    │
│  │ /data/uploads│                       │ /mnt/nas    │    │
│  └─────────────┘                        └─────────────┘    │
│                                                             │
│  Step 2: 메타데이터 업데이트                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ UPDATE TB_FILE                                      │   │
│  │ SET STORAGE_TYPE = 'NAS',                          │   │
│  │     STORAGE_BUCKET = 'taskflow_files'              │   │
│  │ WHERE STORAGE_TYPE = 'LOCAL';                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Step 3: 설정 변경                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ storage:                                            │   │
│  │   type: NAS  # LOCAL → NAS                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Step 4: 서비스 재시작                                      │
│  $ docker-compose restart backend                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 LOCAL/NAS → S3 마이그레이션

```bash
# Step 1: S3 동기화
aws s3 sync /data/uploads s3://taskflow-files/uploads/

# Step 2: 메타데이터 업데이트
UPDATE TB_FILE
SET STORAGE_TYPE = 'S3',
    STORAGE_BUCKET = 'taskflow-files'
WHERE STORAGE_TYPE IN ('LOCAL', 'NAS');

# Step 3: 설정 변경 및 재시작
# application.yml: storage.type: S3
docker-compose restart backend
```

### 8.3 마이그레이션 체크리스트

- [ ] 신규 스토리지 접근 테스트
- [ ] 파일 동기화 완료 확인
- [ ] 파일 개수 검증 (DB vs 스토리지)
- [ ] 메타데이터 업데이트
- [ ] 설정 파일 변경
- [ ] 테스트 환경 검증
- [ ] 서비스 재시작
- [ ] 파일 접근 테스트
- [ ] 구 스토리지 백업 후 정리

---

## 9. 테스트 계획

### 9.1 단위 테스트

| 대상 | 테스트 항목 |
|------|------------|
| LocalFileStorage | 업로드, 다운로드, 삭제, 존재 확인 |
| FileService | 메타데이터 저장, 조회, 삭제 |
| FileController | API 요청/응답, 에러 처리 |

### 9.2 통합 테스트

| 시나리오 | 검증 항목 |
|----------|----------|
| 이미지 업로드 | 파일 저장 + DB 메타데이터 |
| 이미지 조회 | URL 접근 + 바이너리 응답 |
| 마크다운 이미지 | 에디터 삽입 + 미리보기 렌더링 |
| 파일 삭제 | 물리 파일 + 메타데이터 삭제 |

### 9.3 E2E 테스트

| 시나리오 | 단계 |
|----------|------|
| 이미지 업로드 플로우 | 로그인 → 업무 열기 → 에디터 열기 → 이미지 업로드 → 저장 → 미리보기 확인 |
| 드래그앤드롭 | 이미지 파일 드래그 → 에디터 드롭 → 업로드 → 마크다운 삽입 확인 |
| 클립보드 붙여넣기 | 스크린샷 캡처 → Ctrl+V → 업로드 → 마크다운 삽입 확인 |

---

## 10. 성능 고려사항

### 10.1 파일 처리 최적화

| 항목 | 방안 |
|------|------|
| 대용량 파일 | 스트리밍 처리 (메모리 부담 최소화) |
| 이미지 최적화 | 썸네일 생성 (추후) |
| 캐싱 | Cache-Control 헤더 설정 (1년) |
| CDN | S3 + CloudFront 연동 (추후) |

### 10.2 저장 경로 구조

```
/data/uploads/
├── 2024/
│   ├── 12/
│   │   ├── a1b2c3d4-e5f6-7890-abcd-ef1234567890.png
│   │   ├── b2c3d4e5-f6a7-8901-bcde-f23456789012.jpg
│   │   └── ...
│   └── 11/
│       └── ...
└── 2025/
    └── ...
```

- 연/월 기준 디렉토리 분리
- 단일 디렉토리 파일 수 제한 (성능)
- 백업/정리 용이

---

## 11. 용어 정의

| 용어 | 설명 |
|------|------|
| STORAGE_TYPE | 스토리지 종류 (LOCAL, NAS, S3) |
| STORAGE_PATH | 스토리지 내 상대 경로 (마이그레이션 시 변경 불필요) |
| STORAGE_BUCKET | S3 버킷명 또는 NAS 공유명 |
| RELATED_TYPE | 파일이 연관된 엔티티 타입 |
| RELATED_ID | 파일이 연관된 엔티티 ID |

---

## 12. 승인

| 역할 | 이름 | 날짜 | 서명 |
|------|------|------|------|
| 설계자 | | | |
| 검토자 | | | |
| 승인자 | | | |
