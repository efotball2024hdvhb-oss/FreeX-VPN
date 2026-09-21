@echo off
setlocal
set BASE_DIR=%~dp0
if exist "%BASE_DIR%\.gradle-wrapper\gradle-9.3.1\bin\gradle.bat" goto run
where gradle >nul 2>nul && goto system
if not exist "%BASE_DIR%\.gradle-wrapper" mkdir "%BASE_DIR%\.gradle-wrapper"
if not exist "%BASE_DIR%\.gradle-wrapper\gradle.zip" powershell -NoProfile -Command "Invoke-WebRequest -Uri https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -OutFile '%BASE_DIR%\.gradle-wrapper\gradle.zip'"
powershell -NoProfile -Command "Expand-Archive -Force '%BASE_DIR%\.gradle-wrapper\gradle.zip' '%BASE_DIR%\.gradle-wrapper'"
:run
call "%BASE_DIR%\.gradle-wrapper\gradle-9.3.1\bin\gradle.bat" %*
exit /b %errorlevel%
:system
gradle %*
