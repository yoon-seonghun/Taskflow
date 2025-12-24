#!/bin/bash
# ============================================
# TaskFlow 배포본 생성 스크립트
# ============================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 루트 디렉토리
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION="${1:-1.0.0}"
DIST_NAME="taskflow-${VERSION}"
DIST_DIR="${PROJECT_ROOT}/dist/${DIST_NAME}"
ZIP_FILE="${PROJECT_ROOT}/dist/${DIST_NAME}.zip"

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE} TaskFlow 배포본 생성${NC}"
echo -e "${BLUE} Version: ${VERSION}${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# 1. 프론트엔드 빌드
echo -e "${YELLOW}[1/5] 프론트엔드 빌드 중...${NC}"
cd "${PROJECT_ROOT}/frontend"
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}  - npm install 실행 중...${NC}"
    npm install
fi
npm run build
echo -e "${GREEN}  ✓ 프론트엔드 빌드 완료${NC}"
echo ""

# 2. 배포 디렉토리 초기화
echo -e "${YELLOW}[2/5] 배포 디렉토리 준비 중...${NC}"
rm -rf "${DIST_DIR}"
mkdir -p "${DIST_DIR}"
mkdir -p "${DIST_DIR}/backend"
mkdir -p "${DIST_DIR}/frontend"
mkdir -p "${DIST_DIR}/docker/mysql/init"
mkdir -p "${DIST_DIR}/scripts"
echo -e "${GREEN}  ✓ 배포 디렉토리 생성 완료${NC}"
echo ""

# 3. 파일 복사
echo -e "${YELLOW}[3/5] 파일 복사 중...${NC}"

# Backend 소스 복사 (Docker에서 빌드)
echo -e "  - Backend 소스 복사..."
cp -r "${PROJECT_ROOT}/backend/src" "${DIST_DIR}/backend/"
cp -r "${PROJECT_ROOT}/backend/gradle" "${DIST_DIR}/backend/"
cp "${PROJECT_ROOT}/backend/build.gradle.kts" "${DIST_DIR}/backend/"
cp "${PROJECT_ROOT}/backend/settings.gradle.kts" "${DIST_DIR}/backend/"
[ -f "${PROJECT_ROOT}/backend/gradlew" ] && cp "${PROJECT_ROOT}/backend/gradlew" "${DIST_DIR}/backend/"
[ -f "${PROJECT_ROOT}/backend/gradlew.bat" ] && cp "${PROJECT_ROOT}/backend/gradlew.bat" "${DIST_DIR}/backend/"
cp "${PROJECT_ROOT}/backend/Dockerfile" "${DIST_DIR}/backend/"

# Frontend 소스 및 빌드 결과 복사
echo -e "  - Frontend 소스 복사..."
cp -r "${PROJECT_ROOT}/frontend/src" "${DIST_DIR}/frontend/"
cp -r "${PROJECT_ROOT}/frontend/public" "${DIST_DIR}/frontend/" 2>/dev/null || true
cp -r "${PROJECT_ROOT}/frontend/dist" "${DIST_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/package.json" "${DIST_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/package-lock.json" "${DIST_DIR}/frontend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/frontend/vite.config.ts" "${DIST_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/tsconfig.json" "${DIST_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/tsconfig.node.json" "${DIST_DIR}/frontend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/frontend/index.html" "${DIST_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/nginx.conf" "${DIST_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/Dockerfile" "${DIST_DIR}/frontend/"
# Tailwind/PostCSS 설정 파일 (Docker 빌드 시 필요)
cp "${PROJECT_ROOT}/frontend/tailwind.config.js" "${DIST_DIR}/frontend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/frontend/postcss.config.js" "${DIST_DIR}/frontend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/frontend/env.d.ts" "${DIST_DIR}/frontend/" 2>/dev/null || true

# Docker 관련 파일 복사
echo -e "  - Docker 설정 복사..."
cp "${PROJECT_ROOT}/docker-compose.yml" "${DIST_DIR}/"
cp "${PROJECT_ROOT}/.env.example" "${DIST_DIR}/"
cp -r "${PROJECT_ROOT}/docker/mysql/init/"* "${DIST_DIR}/docker/mysql/init/"

# 스크립트 복사 (프로젝트 내 scripts 폴더가 있으면 복사)
echo -e "  - 스크립트 복사..."
if [ -d "${PROJECT_ROOT}/scripts" ]; then
    cp -r "${PROJECT_ROOT}/scripts/"* "${DIST_DIR}/scripts/" 2>/dev/null || true
fi

# 문서 복사
echo -e "  - 문서 복사..."
if [ -f "${PROJECT_ROOT}/DEPLOYMENT.md" ]; then
    cp "${PROJECT_ROOT}/DEPLOYMENT.md" "${DIST_DIR}/"
fi

echo -e "${GREEN}  ✓ 파일 복사 완료${NC}"
echo ""

# 4. 불필요한 파일 제거
echo -e "${YELLOW}[4/5] 불필요한 파일 정리 중...${NC}"
find "${DIST_DIR}" -name "node_modules" -type d -exec rm -rf {} + 2>/dev/null || true
find "${DIST_DIR}" -name ".git" -type d -exec rm -rf {} + 2>/dev/null || true
find "${DIST_DIR}" -name ".gitignore" -type f -delete 2>/dev/null || true
find "${DIST_DIR}" -name "*.log" -type f -delete 2>/dev/null || true
find "${DIST_DIR}" -name ".DS_Store" -type f -delete 2>/dev/null || true
echo -e "${GREEN}  ✓ 정리 완료${NC}"
echo ""

# 5. 압축 파일 생성
echo -e "${YELLOW}[5/5] 압축 파일 생성 중...${NC}"
cd "${PROJECT_ROOT}/dist"
ARCHIVE_FILE="${DIST_NAME}.tar.gz"
rm -f "${ARCHIVE_FILE}"
tar -czvf "${ARCHIVE_FILE}" "${DIST_NAME}" > /dev/null
echo -e "${GREEN}  ✓ 압축 파일 생성 완료${NC}"
echo ""

# 결과 출력
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN} 배포본 생성 완료!${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo -e "배포 디렉토리: ${DIST_DIR}"
echo -e "압축 파일: ${PROJECT_ROOT}/dist/${ARCHIVE_FILE}"
echo ""
echo -e "압축 파일 크기: $(du -h "${ARCHIVE_FILE}" | cut -f1)"
echo ""
echo -e "${YELLOW}배포 방법:${NC}"
echo -e "  1. 압축 파일을 서버로 전송"
echo -e "  2. 압축 해제: tar -xzvf ${ARCHIVE_FILE}"
echo -e "  3. 디렉토리 이동: cd ${DIST_NAME}"
echo -e "  4. 환경설정: cp .env.example .env && vi .env"
echo -e "  5. 실행: docker compose up -d --build"
echo ""
