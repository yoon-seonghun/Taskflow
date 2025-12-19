@echo off
REM ============================================
REM TaskFlow 서비스 시작 스크립트 (Windows)
REM ============================================

setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

cd /d "%PROJECT_ROOT%"

REM .env 파일 확인
if not exist ".env" (
    echo 오류: .env 파일이 없습니다.
    echo 다음 명령으로 생성하세요: copy .env.example .env
    exit /b 1
)

echo ============================================
echo TaskFlow 서비스 시작
echo ============================================

REM Docker Compose 실행
docker-compose up -d --build

echo.
echo 서비스 시작 완료!
echo.
echo 접속 URL:
echo   - Frontend: http://localhost:3000
echo   - Backend:  http://localhost:8080/api
echo.
echo 기본 계정: admin / admin123!
echo.
echo 로그 확인: docker-compose logs -f

endlocal
