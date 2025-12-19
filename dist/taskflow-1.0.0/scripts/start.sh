#!/bin/bash
# ============================================
# TaskFlow 서비스 시작 스크립트
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# .env 파일 확인
if [ ! -f ".env" ]; then
    echo "오류: .env 파일이 없습니다."
    echo "다음 명령으로 생성하세요: cp .env.example .env"
    exit 1
fi

# JWT_SECRET 확인
if grep -q "your-base64-encoded-secret-key" .env; then
    echo "경고: JWT_SECRET이 기본값입니다. 보안을 위해 변경하세요!"
    echo "생성 방법: openssl rand -base64 32"
    read -p "계속하시겠습니까? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

echo "============================================"
echo "TaskFlow 서비스 시작"
echo "============================================"

# Docker Compose 실행
docker-compose up -d --build

echo ""
echo "서비스 시작 완료!"
echo ""
echo "접속 URL:"
source .env
echo "  - Frontend: http://localhost:${FRONTEND_PORT:-3000}"
echo "  - Backend:  http://localhost:${BACKEND_PORT:-8080}/api"
echo ""
echo "기본 계정: admin / admin123!"
echo ""
echo "로그 확인: docker-compose logs -f"
