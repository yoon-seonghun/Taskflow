# Taskflow

> 업무관리용 애플리케이션 (Notion Style)

Notion 스타일의 직관적인 UI를 제공하는 업무 관리 시스템입니다.

## 📋 프로젝트 개요

Taskflow는 팀과 개인의 업무를 효율적으로 관리할 수 있는 웹 애플리케이션입니다. Notion의 유연하고 직관적인 인터페이스에서 영감을 받아 개발되었습니다.

## 🛠️ 기술 스택

### Frontend
- **Vue.js** - 프론트엔드 프레임워크
- **TypeScript** - 타입 안정성 확보

### Backend
- **Java** - 백엔드 언어
- **Spring Boot** (추정) - 백엔드 프레임워크

### Infrastructure
- **Docker** - 컨테이너화
- **MySQL** - 데이터베이스
- **Docker Compose** - 멀티 컨테이너 오케스트레이션

## 📁 프로젝트 구조

```
Taskflow/
├── .claude/agents/     # Claude AI 에이전트 설정
├── backend/            # Java 백엔드 소스
├── frontend/           # Vue.js 프론트엔드 소스
├── dist/               # 빌드 결과물
├── docker/
│   └── mysql/init/     # MySQL 초기화 스크립트
├── docs/               # 프로젝트 문서
├── scripts/            # 유틸리티 스크립트
├── .env.example        # 환경변수 예시
├── docker-compose.yml  # Docker Compose 설정
├── DEPLOYMENT.md       # 배포 가이드
└── CLAUDE.md           # Claude 개발 가이드
```

## 🚀 시작하기

### 사전 요구사항

- Docker & Docker Compose
- Node.js (프론트엔드 개발용)
- Java 11+ (백엔드 개발용)

### 설치 및 실행

1. **리포지토리 클론**
   ```bash
   git clone https://github.com/yoon-seonghun/Taskflow.git
   cd Taskflow
   ```

2. **환경변수 설정**
   ```bash
   cp .env.example .env
   # .env 파일을 열어 필요한 값들을 설정
   ```

3. **Docker로 실행**
   ```bash
   docker-compose up -d
   ```

### 개발 환경 실행

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

**Backend:**
```bash
cd backend
./gradlew bootRun
```

## 📦 배포

### 배포본 생성

배포용 압축 파일을 생성하는 스크립트가 제공됩니다.

**Linux/Mac/WSL:**
```bash
# 기본 버전 (1.0.0)
./build-release.sh

# 버전 지정
./build-release.sh 1.1.0
```

**Windows PowerShell:**
```powershell
# 기본 버전 (1.0.0)
.\build-release.ps1

# 버전 지정
.\build-release.ps1 -Version 1.1.0
```

### 배포본 구조

```
taskflow-{version}/
├── backend/              # 백엔드 소스 (Docker에서 빌드)
├── frontend/             # 프론트엔드 소스 + 빌드 결과물
├── docker/
│   └── mysql/init/       # DB 초기화 스크립트
├── scripts/
│   ├── start.sh          # 시작 스크립트
│   └── stop.sh           # 중지 스크립트
├── docker-compose.yml
└── .env.example
```

### 서버 배포 방법

1. **압축 파일 서버로 전송**
   ```bash
   scp dist/taskflow-1.0.0.tar.gz user@server:/path/to/
   ```

2. **서버에서 압축 해제**
   ```bash
   tar -xzvf taskflow-1.0.0.tar.gz
   cd taskflow-1.0.0
   ```

3. **환경 설정**
   ```bash
   cp .env.example .env
   vi .env
   ```

   주요 설정 항목:
   | 항목 | 설명 | 예시 |
   |------|------|------|
   | `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 | `your_secure_password` |
   | `MYSQL_PASSWORD` | 앱 DB 비밀번호 | `your_app_password` |
   | `JWT_SECRET` | JWT 시크릿 키 (Base64) | `openssl rand -base64 32` |
   | `CORS_ALLOWED_ORIGINS` | 프론트엔드 접속 URL | `http://서버IP:9800` |
   | `FRONTEND_PORT` | 프론트엔드 포트 | `9800` |
   | `BACKEND_PORT` | 백엔드 API 포트 | `8080` |

4. **Docker 실행**
   ```bash
   # 스크립트 사용
   ./scripts/start.sh

   # 또는 직접 실행
   docker compose up -d --build
   ```

5. **상태 확인**
   ```bash
   docker compose ps
   docker compose logs -f
   ```

### 서비스 관리

```bash
# 시작
./scripts/start.sh
# 또는
docker compose up -d

# 중지
./scripts/stop.sh
# 또는
docker compose down

# 로그 확인
docker compose logs -f backend
docker compose logs -f frontend

# 재시작
docker compose restart
```

## 📖 문서

- [배포 가이드](./DEPLOYMENT.md)
- [개발 지시서](./TaskFlow_Claude_Code_개발지시서.md)
- [개발 주의사항](./개발%20주의사항.md)
- [개발 노트](./dev_note.md)

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트의 라이선스 정보는 리포지토리를 확인해주세요.

## 📞 Contact

- GitHub: [@yoon-seonghun](https://github.com/yoon-seonghun)

---

⭐ 이 프로젝트가 도움이 되셨다면 Star를 눌러주세요!
