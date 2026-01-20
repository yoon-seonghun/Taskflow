#!/bin/bash
# TaskFlow 중지 스크립트

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "TaskFlow 중지 중..."
docker compose down

echo "TaskFlow가 중지되었습니다."
