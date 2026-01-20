# TaskFlow 배포 가이드

## 목차

1. [사전 요구사항](#사전-요구사항)
2. [패키징](#패키징)
3. [타 시스템 이식](#타-시스템-이식)
4. [환경변수 설정](#환경변수-설정)
5. [서비스 관리](#서비스-관리)
6. [문제 해결](#문제-해결)

---

## 사전 요구사항

### 대상 서버 요구사항

| 항목 | 최소 사양 | 권장 사양 |
|------|----------|----------|
| OS | Linux/Windows/macOS | Linux (Ubuntu 20.04+) |
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |
| RAM | 4GB | 8GB+ |
| Disk | 10GB | 20GB+ |

### Docker 설치 확인

```bash
docker --version
docker-compose --version
```

---

## 패키징

### Linux/macOS

```bash
# 프로젝트 디렉토리에서 실행
./scripts/package.sh 1.0.0

# 결과물 확인
ls -la dist/
# taskflow-1.0.0.tar.gz
# taskflow-1.0.0.zip
```

### Windows

```cmd
REM 프로젝트 디렉토리에서 실행
scripts\package.bat 1.0.0

REM 결과물 확인
dir dist\
REM taskflow-1.0.0.zip
```

### 패키지 구조

```
taskflow-1.0.0/
├── backend/                 # Spring Boot 백엔드
│   ├── src/
│   ├── build.gradle.kts
│   └── Dockerfile
├── frontend/                # Vue.js 프론트엔드
│   ├── src/
│   ├── package.json
│   ├── nginx.conf
│   └── Dockerfile
├── docker/
│   └── mysql/
│       └── init/            # DB 초기화 스크립트
│           ├── 01_schema.sql
│           └── 02_init_data.sql
├── scripts/                 # 관리 스크립트
│   ├── start.sh / start.bat
│   └── stop.sh / stop.bat
├── docker-compose.yml
├── .env.example
└── DEPLOYMENT.md
```

---

## 타 시스템 이식

### 방법 1: 패키지 파일 전송 (권장)

#### 1단계: 패키지 전송

```bash
# SCP로 전송
scp dist/taskflow-1.0.0.tar.gz user@target-server:/home/user/

# 또는 SFTP, USB 등 사용
```

#### 2단계: 압축 해제

```bash
# Linux/macOS
cd /home/user
tar -xzvf taskflow-1.0.0.tar.gz
cd taskflow-1.0.0

# Windows (PowerShell)
Expand-Archive -Path taskflow-1.0.0.zip -DestinationPath .
cd taskflow-1.0.0
```

#### 3단계: 환경변수 설정

```bash
# .env 파일 생성
cp .env.example .env

# .env 파일 편집
nano .env   # Linux
notepad .env  # Windows
```

**필수 변경 항목:**
```env
# JWT 시크릿 키 (반드시 변경!)
JWT_SECRET=생성된_랜덤_키

# 포트 변경 시 (선택)
FRONTEND_PORT=3000
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

**JWT_SECRET 생성:**
```bash
# Linux/macOS
openssl rand -base64 32

# Windows (PowerShell)
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

#### 4단계: 서비스 시작

```bash
# Linux/macOS
./scripts/start.sh

# Windows
scripts\start.bat

# 또는 직접 실행
docker-compose up -d --build
```

#### 5단계: 접속 확인

- Frontend: http://서버IP:3000
- 기본 계정: `admin` / `admin123!`

---

### 방법 2: Git Clone (개발 환경)

```bash
# 저장소 클론
git clone <repository-url> taskflow
cd taskflow

# 환경변수 설정
cp .env.example .env
# .env 편집...

# 서비스 시작
docker-compose up -d --build
```

---

## 환경변수 설정

### 전체 환경변수 목록

| 변수명 | 필수 | 기본값 | 설명 |
|--------|:----:|--------|------|
| **데이터베이스** |
| MYSQL_ROOT_PASSWORD | O | root | MySQL root 비밀번호 |
| MYSQL_DATABASE | O | taskflow | 데이터베이스명 |
| MYSQL_USER | O | taskflow | 애플리케이션 DB 사용자 |
| MYSQL_PASSWORD | O | taskflow123 | DB 사용자 비밀번호 |
| **인증** |
| JWT_SECRET | O | (필수 변경) | JWT 서명 키 (Base64) |
| **포트** |
| MYSQL_PORT | - | 3306 | MySQL 외부 포트 |
| BACKEND_PORT | - | 8080 | Backend 외부 포트 |
| FRONTEND_PORT | - | 3000 | Frontend 외부 포트 |
| **보안** |
| CORS_ALLOWED_ORIGINS | - | http://localhost:3000 | 허용 Origin |
| COOKIE_SECURE | - | false | HTTPS 사용 시 true |
| **시스템** |
| TZ | - | Asia/Seoul | 시간대 |

### 포트 변경 시 주의사항

```env
# FRONTEND_PORT 변경 시 CORS도 함께 변경!
FRONTEND_PORT=4000
CORS_ALLOWED_ORIGINS=http://localhost:4000

# 외부 도메인 사용 시
CORS_ALLOWED_ORIGINS=https://taskflow.example.com
```

### 프로덕션 권장 설정

```env
# 강력한 비밀번호 사용
MYSQL_ROOT_PASSWORD=매우_강력한_비밀번호
MYSQL_PASSWORD=강력한_비밀번호
JWT_SECRET=openssl_rand로_생성한_키

# HTTPS 환경
COOKIE_SECURE=true
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

---

## 서비스 관리

### 시작/중지

```bash
# 시작
./scripts/start.sh          # Linux/macOS
scripts\start.bat           # Windows
docker-compose up -d        # 직접 실행

# 중지
./scripts/stop.sh           # Linux/macOS
scripts\stop.bat            # Windows
docker-compose down         # 직접 실행

# 재시작
docker-compose restart
```

### 로그 확인

```bash
# 전체 로그
docker-compose logs -f

# 서비스별 로그
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mysql
```

### 상태 확인

```bash
docker-compose ps
```

### 재빌드 (코드 변경 후)

```bash
# 전체 재빌드
docker-compose up -d --build

# 특정 서비스만
docker-compose up -d --build backend
```

### 데이터 초기화

```bash
# 컨테이너 + 볼륨 삭제 (DB 데이터 삭제됨!)
docker-compose down -v

# 다시 시작
docker-compose up -d --build
```

---

## 문제 해결

### 1. MySQL 연결 실패

**증상:** Backend가 시작되지 않고 DB 연결 에러 발생

**해결:**
```bash
# MySQL 상태 확인
docker-compose ps
docker-compose logs mysql

# MySQL이 완전히 시작될 때까지 대기 (healthcheck)
# 보통 30초~1분 소요
```

### 2. 포트 충돌

**증상:** `port is already allocated` 에러

**해결:**
```bash
# 사용 중인 포트 확인
# Linux/macOS
lsof -i :3306
lsof -i :8080
lsof -i :3000

# Windows
netstat -ano | findstr :3306

# .env에서 포트 변경
MYSQL_PORT=13306
BACKEND_PORT=18080
FRONTEND_PORT=13000
```

### 3. CORS 에러

**증상:** 브라우저 콘솔에 CORS 에러 표시

**해결:**
```env
# .env에서 CORS 설정 확인
# Frontend 접속 URL과 동일해야 함
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### 4. 권한 에러 (Linux)

**증상:** `permission denied` 에러

**해결:**
```bash
# Docker 그룹에 사용자 추가
sudo usermod -aG docker $USER

# 재로그인 필요
exit
# 다시 로그인

# 스크립트 실행 권한
chmod +x scripts/*.sh
```

### 5. 메모리 부족

**증상:** 컨테이너가 자꾸 죽음

**해결:**
```bash
# 메모리 확인
free -h

# Docker 리소스 정리
docker system prune -a
```

### 6. 빌드 실패

**증상:** `docker-compose build` 실패

**해결:**
```bash
# 캐시 없이 재빌드
docker-compose build --no-cache

# 이미지 정리 후 재시도
docker image prune -a
docker-compose up -d --build
```

---

## 기본 계정 정보

| 구분 | 아이디 | 비밀번호 | 비고 |
|------|--------|----------|------|
| 관리자 | admin | admin123! | 최초 로그인 후 변경 권장 |
| 테스트 | user1 | user123! | 테스트용 계정 |

---

## 버전 정보

- 문서 버전: 1.0
- 최종 수정: 2024-12
