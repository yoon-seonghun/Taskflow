# ============================================
# TaskFlow 배포본 생성 스크립트 (Windows PowerShell)
# ============================================

param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

# 프로젝트 루트 디렉토리
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$DistName = "taskflow-$Version"
$DistDir = Join-Path $ProjectRoot "dist\$DistName"
$ZipFile = Join-Path $ProjectRoot "dist\$DistName.zip"

Write-Host "============================================" -ForegroundColor Blue
Write-Host " TaskFlow 배포본 생성" -ForegroundColor Blue
Write-Host " Version: $Version" -ForegroundColor Blue
Write-Host "============================================" -ForegroundColor Blue
Write-Host ""

# 1. 프론트엔드 빌드
Write-Host "[1/5] 프론트엔드 빌드 중..." -ForegroundColor Yellow
Set-Location "$ProjectRoot\frontend"
if (!(Test-Path "node_modules")) {
    Write-Host "  - npm install 실행 중..." -ForegroundColor Yellow
    npm install
}
npm run build
Write-Host "  v 프론트엔드 빌드 완료" -ForegroundColor Green
Write-Host ""

# 2. 배포 디렉토리 초기화
Write-Host "[2/5] 배포 디렉토리 준비 중..." -ForegroundColor Yellow
if (Test-Path $DistDir) {
    Remove-Item -Recurse -Force $DistDir
}
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null
New-Item -ItemType Directory -Force -Path "$DistDir\backend" | Out-Null
New-Item -ItemType Directory -Force -Path "$DistDir\frontend" | Out-Null
New-Item -ItemType Directory -Force -Path "$DistDir\docker\mysql\init" | Out-Null
New-Item -ItemType Directory -Force -Path "$DistDir\scripts" | Out-Null
Write-Host "  v 배포 디렉토리 생성 완료" -ForegroundColor Green
Write-Host ""

# 3. 파일 복사
Write-Host "[3/5] 파일 복사 중..." -ForegroundColor Yellow

# Backend 소스 복사
Write-Host "  - Backend 소스 복사..."
Copy-Item -Recurse "$ProjectRoot\backend\src" "$DistDir\backend\"
Copy-Item -Recurse "$ProjectRoot\backend\gradle" "$DistDir\backend\"
Copy-Item "$ProjectRoot\backend\build.gradle.kts" "$DistDir\backend\"
Copy-Item "$ProjectRoot\backend\settings.gradle.kts" "$DistDir\backend\"
if (Test-Path "$ProjectRoot\backend\gradlew") {
    Copy-Item "$ProjectRoot\backend\gradlew" "$DistDir\backend\"
}
if (Test-Path "$ProjectRoot\backend\gradlew.bat") {
    Copy-Item "$ProjectRoot\backend\gradlew.bat" "$DistDir\backend\"
}
Copy-Item "$ProjectRoot\backend\Dockerfile" "$DistDir\backend\"

# Frontend 소스 및 빌드 결과 복사
Write-Host "  - Frontend 소스 복사..."
Copy-Item -Recurse "$ProjectRoot\frontend\src" "$DistDir\frontend\"
if (Test-Path "$ProjectRoot\frontend\public") {
    Copy-Item -Recurse "$ProjectRoot\frontend\public" "$DistDir\frontend\"
}
Copy-Item -Recurse "$ProjectRoot\frontend\dist" "$DistDir\frontend\"
Copy-Item "$ProjectRoot\frontend\package.json" "$DistDir\frontend\"
if (Test-Path "$ProjectRoot\frontend\package-lock.json") {
    Copy-Item "$ProjectRoot\frontend\package-lock.json" "$DistDir\frontend\"
}
Copy-Item "$ProjectRoot\frontend\vite.config.ts" "$DistDir\frontend\"
Copy-Item "$ProjectRoot\frontend\tsconfig.json" "$DistDir\frontend\"
if (Test-Path "$ProjectRoot\frontend\tsconfig.node.json") {
    Copy-Item "$ProjectRoot\frontend\tsconfig.node.json" "$DistDir\frontend\"
}
Copy-Item "$ProjectRoot\frontend\index.html" "$DistDir\frontend\"
Copy-Item "$ProjectRoot\frontend\nginx.conf" "$DistDir\frontend\"
Copy-Item "$ProjectRoot\frontend\Dockerfile" "$DistDir\frontend\"

# Docker 관련 파일 복사
Write-Host "  - Docker 설정 복사..."
Copy-Item "$ProjectRoot\docker-compose.yml" "$DistDir\"
Copy-Item "$ProjectRoot\.env.example" "$DistDir\"
Copy-Item "$ProjectRoot\docker\mysql\init\*" "$DistDir\docker\mysql\init\"

# 스크립트 복사
Write-Host "  - 스크립트 복사..."
if (Test-Path "$ProjectRoot\scripts") {
    Copy-Item "$ProjectRoot\scripts\*" "$DistDir\scripts\" -ErrorAction SilentlyContinue
}

# 문서 복사
Write-Host "  - 문서 복사..."
if (Test-Path "$ProjectRoot\DEPLOYMENT.md") {
    Copy-Item "$ProjectRoot\DEPLOYMENT.md" "$DistDir\"
}

Write-Host "  v 파일 복사 완료" -ForegroundColor Green
Write-Host ""

# 4. 불필요한 파일 제거
Write-Host "[4/5] 불필요한 파일 정리 중..." -ForegroundColor Yellow
Get-ChildItem -Path $DistDir -Recurse -Directory -Filter "node_modules" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path $DistDir -Recurse -Directory -Filter ".git" | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path $DistDir -Recurse -File -Filter ".gitignore" | Remove-Item -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path $DistDir -Recurse -File -Filter "*.log" | Remove-Item -Force -ErrorAction SilentlyContinue
Write-Host "  v 정리 완료" -ForegroundColor Green
Write-Host ""

# 5. ZIP 파일 생성
Write-Host "[5/5] ZIP 파일 생성 중..." -ForegroundColor Yellow
Set-Location "$ProjectRoot\dist"
if (Test-Path $ZipFile) {
    Remove-Item -Force $ZipFile
}
Compress-Archive -Path $DistName -DestinationPath $ZipFile
Write-Host "  v ZIP 파일 생성 완료" -ForegroundColor Green
Write-Host ""

# 결과 출력
Write-Host "============================================" -ForegroundColor Blue
Write-Host " 배포본 생성 완료!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Blue
Write-Host ""
Write-Host "배포 디렉토리: $DistDir"
Write-Host "ZIP 파일: $ZipFile"
Write-Host ""
$zipSize = (Get-Item $ZipFile).Length / 1MB
Write-Host ("ZIP 파일 크기: {0:N2} MB" -f $zipSize)
Write-Host ""
Write-Host "배포 방법:" -ForegroundColor Yellow
Write-Host "  1. ZIP 파일을 서버로 전송"
Write-Host "  2. 압축 해제: unzip $DistName.zip"
Write-Host "  3. 디렉토리 이동: cd $DistName"
Write-Host "  4. 환경설정: cp .env.example .env && vi .env"
Write-Host "  5. 실행: docker compose up -d --build"
Write-Host ""

Set-Location $ProjectRoot
