#!/bin/bash
# TaskFlow 시작 스크립트

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# .env 파일 확인
if [ ! -f ".env" ]; then
    echo "Error: .env 파일이 없습니다."
    echo "cp .env.example .env 명령으로 생성 후 설정을 수정하세요."
    exit 1
fi

echo "TaskFlow 시작 중..."
docker compose up -d --build

echo ""
echo "컨테이너 상태:"
docker compose ps

echo ""
echo "TaskFlow가 시작되었습니다."
