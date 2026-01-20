@echo off
REM ============================================
REM TaskFlow 서비스 중지 스크립트 (Windows)
REM ============================================

setlocal

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..

cd /d "%PROJECT_ROOT%"

echo ============================================
echo TaskFlow 서비스 중지
echo ============================================

docker-compose down

echo.
echo 서비스가 중지되었습니다.
echo.
echo 데이터를 포함하여 완전히 삭제하려면:
echo   docker-compose down -v

endlocal
