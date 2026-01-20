@echo off
REM ============================================
REM TaskFlow 배포 패키지 생성 스크립트 (Windows)
REM ============================================
REM 사용법: scripts\package.bat [버전]
REM 예시: scripts\package.bat 1.0.0

setlocal enabledelayedexpansion

REM 버전 설정
if "%1"=="" (
    for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set VERSION=%%c%%a%%b
) else (
    set VERSION=%1
)

set PACKAGE_NAME=taskflow-%VERSION%
set PROJECT_ROOT=%~dp0..
set OUTPUT_DIR=%PROJECT_ROOT%\dist
set PACKAGE_DIR=%OUTPUT_DIR%\%PACKAGE_NAME%

echo ============================================
echo TaskFlow 배포 패키지 생성
echo 버전: %VERSION%
echo ============================================

REM 기존 패키지 삭제
if exist "%PACKAGE_DIR%" rmdir /s /q "%PACKAGE_DIR%"
if exist "%OUTPUT_DIR%\%PACKAGE_NAME%.zip" del "%OUTPUT_DIR%\%PACKAGE_NAME%.zip"

REM 패키지 디렉토리 생성
mkdir "%PACKAGE_DIR%"

echo [1/5] Backend 복사 중...
mkdir "%PACKAGE_DIR%\backend"
xcopy /e /i /q "%PROJECT_ROOT%\backend\src" "%PACKAGE_DIR%\backend\src"
xcopy /e /i /q "%PROJECT_ROOT%\backend\gradle" "%PACKAGE_DIR%\backend\gradle" 2>nul
copy "%PROJECT_ROOT%\backend\build.gradle.kts" "%PACKAGE_DIR%\backend\" >nul
copy "%PROJECT_ROOT%\backend\settings.gradle.kts" "%PACKAGE_DIR%\backend\" >nul
copy "%PROJECT_ROOT%\backend\gradlew" "%PACKAGE_DIR%\backend\" 2>nul
copy "%PROJECT_ROOT%\backend\gradlew.bat" "%PACKAGE_DIR%\backend\" 2>nul
copy "%PROJECT_ROOT%\backend\Dockerfile" "%PACKAGE_DIR%\backend\" >nul

echo [2/5] Frontend 복사 중...
mkdir "%PACKAGE_DIR%\frontend"
xcopy /e /i /q "%PROJECT_ROOT%\frontend\src" "%PACKAGE_DIR%\frontend\src"
xcopy /e /i /q "%PROJECT_ROOT%\frontend\public" "%PACKAGE_DIR%\frontend\public"
copy "%PROJECT_ROOT%\frontend\package.json" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\package-lock.json" "%PACKAGE_DIR%\frontend\" 2>nul
copy "%PROJECT_ROOT%\frontend\tsconfig.json" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\tsconfig.node.json" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\vite.config.ts" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\tailwind.config.js" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\postcss.config.js" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\index.html" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\env.d.ts" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\nginx.conf" "%PACKAGE_DIR%\frontend\" >nul
copy "%PROJECT_ROOT%\frontend\Dockerfile" "%PACKAGE_DIR%\frontend\" >nul

echo [3/5] Docker 설정 복사 중...
mkdir "%PACKAGE_DIR%\docker\mysql\init"
copy "%PROJECT_ROOT%\docker\mysql\init\01_schema.sql" "%PACKAGE_DIR%\docker\mysql\init\" >nul
copy "%PROJECT_ROOT%\docker\mysql\init\02_init_data.sql" "%PACKAGE_DIR%\docker\mysql\init\" >nul

echo [4/5] 설정 파일 복사 중...
copy "%PROJECT_ROOT%\docker-compose.yml" "%PACKAGE_DIR%\" >nul
copy "%PROJECT_ROOT%\.env.example" "%PACKAGE_DIR%\" >nul
copy "%PROJECT_ROOT%\DEPLOYMENT.md" "%PACKAGE_DIR%\" >nul

REM scripts 디렉토리 복사
mkdir "%PACKAGE_DIR%\scripts"
copy "%PROJECT_ROOT%\scripts\start.bat" "%PACKAGE_DIR%\scripts\" 2>nul
copy "%PROJECT_ROOT%\scripts\stop.bat" "%PACKAGE_DIR%\scripts\" 2>nul
copy "%PROJECT_ROOT%\scripts\start.sh" "%PACKAGE_DIR%\scripts\" 2>nul
copy "%PROJECT_ROOT%\scripts\stop.sh" "%PACKAGE_DIR%\scripts\" 2>nul

echo [5/5] 패키지 압축 중...
cd "%OUTPUT_DIR%"

REM PowerShell로 ZIP 생성
powershell -command "Compress-Archive -Path '%PACKAGE_NAME%' -DestinationPath '%PACKAGE_NAME%.zip' -Force"

echo.
echo ============================================
echo 패키지 생성 완료!
echo ============================================
echo 위치: %OUTPUT_DIR%
echo.
dir "%OUTPUT_DIR%\%PACKAGE_NAME%*"
echo.
echo 이식 방법:
echo 1. 패키지 파일을 대상 서버로 전송
echo 2. 압축 해제
echo 3. cd %PACKAGE_NAME%
echo 4. copy .env.example .env
echo 5. .env 파일 편집 (JWT_SECRET 필수 변경)
echo 6. docker-compose up -d --build

endlocal
