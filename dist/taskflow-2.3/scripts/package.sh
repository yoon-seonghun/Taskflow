#!/bin/bash
# ============================================
# TaskFlow 배포 패키지 생성 스크립트
# ============================================
# 사용법: ./scripts/package.sh [버전]
# 예시: ./scripts/package.sh 1.0.0

set -e

# 버전 설정
VERSION=${1:-$(date +%Y%m%d)}
PACKAGE_NAME="taskflow-${VERSION}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
OUTPUT_DIR="${PROJECT_ROOT}/dist"
PACKAGE_DIR="${OUTPUT_DIR}/${PACKAGE_NAME}"

echo "============================================"
echo "TaskFlow 배포 패키지 생성"
echo "버전: ${VERSION}"
echo "============================================"

# 기존 패키지 삭제
rm -rf "${PACKAGE_DIR}"
rm -f "${OUTPUT_DIR}/${PACKAGE_NAME}.tar.gz"
rm -f "${OUTPUT_DIR}/${PACKAGE_NAME}.zip"

# 패키지 디렉토리 생성
mkdir -p "${PACKAGE_DIR}"

echo "[1/5] Backend 복사 중..."
mkdir -p "${PACKAGE_DIR}/backend"
cp -r "${PROJECT_ROOT}/backend/src" "${PACKAGE_DIR}/backend/"
cp -r "${PROJECT_ROOT}/backend/gradle" "${PACKAGE_DIR}/backend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/backend/build.gradle.kts" "${PACKAGE_DIR}/backend/"
cp "${PROJECT_ROOT}/backend/settings.gradle.kts" "${PACKAGE_DIR}/backend/"
cp "${PROJECT_ROOT}/backend/gradlew" "${PACKAGE_DIR}/backend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/backend/gradlew.bat" "${PACKAGE_DIR}/backend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/backend/Dockerfile" "${PACKAGE_DIR}/backend/"

echo "[2/5] Frontend 복사 중..."
mkdir -p "${PACKAGE_DIR}/frontend"
cp -r "${PROJECT_ROOT}/frontend/src" "${PACKAGE_DIR}/frontend/"
cp -r "${PROJECT_ROOT}/frontend/public" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/package.json" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/package-lock.json" "${PACKAGE_DIR}/frontend/" 2>/dev/null || true
cp "${PROJECT_ROOT}/frontend/tsconfig.json" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/tsconfig.node.json" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/vite.config.ts" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/tailwind.config.js" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/postcss.config.js" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/index.html" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/env.d.ts" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/nginx.conf" "${PACKAGE_DIR}/frontend/"
cp "${PROJECT_ROOT}/frontend/Dockerfile" "${PACKAGE_DIR}/frontend/"

echo "[3/5] Docker 설정 복사 중..."
mkdir -p "${PACKAGE_DIR}/docker/mysql/init"
cp "${PROJECT_ROOT}/docker/mysql/init/01_schema.sql" "${PACKAGE_DIR}/docker/mysql/init/"
cp "${PROJECT_ROOT}/docker/mysql/init/02_init_data.sql" "${PACKAGE_DIR}/docker/mysql/init/"

echo "[4/5] 설정 파일 복사 중..."
cp "${PROJECT_ROOT}/docker-compose.yml" "${PACKAGE_DIR}/"
cp "${PROJECT_ROOT}/.env.example" "${PACKAGE_DIR}/"
cp "${PROJECT_ROOT}/DEPLOYMENT.md" "${PACKAGE_DIR}/"

# scripts 디렉토리 복사
mkdir -p "${PACKAGE_DIR}/scripts"
cp "${PROJECT_ROOT}/scripts/start.sh" "${PACKAGE_DIR}/scripts/" 2>/dev/null || true
cp "${PROJECT_ROOT}/scripts/stop.sh" "${PACKAGE_DIR}/scripts/" 2>/dev/null || true

echo "[5/5] 패키지 압축 중..."
cd "${OUTPUT_DIR}"

# tar.gz 생성
tar -czvf "${PACKAGE_NAME}.tar.gz" "${PACKAGE_NAME}"

# zip 생성 (Windows 호환)
if command -v zip &> /dev/null; then
    zip -r "${PACKAGE_NAME}.zip" "${PACKAGE_NAME}"
fi

# 패키지 디렉토리 정리 (압축 파일만 유지)
# rm -rf "${PACKAGE_DIR}"

echo ""
echo "============================================"
echo "패키지 생성 완료!"
echo "============================================"
echo "위치: ${OUTPUT_DIR}"
echo ""
ls -lh "${OUTPUT_DIR}/${PACKAGE_NAME}"*
echo ""
echo "이식 방법:"
echo "1. 패키지 파일을 대상 서버로 전송"
echo "2. 압축 해제: tar -xzvf ${PACKAGE_NAME}.tar.gz"
echo "3. cd ${PACKAGE_NAME}"
echo "4. cp .env.example .env"
echo "5. .env 파일 편집 (JWT_SECRET 필수 변경)"
echo "6. docker-compose up -d --build"
