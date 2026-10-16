@echo off
setlocal
set "GRADLE_VERSION=8.9"
set "GRADLE_BASE=%USERPROFILE%\.gradle\gympilot-distributions"
set "GRADLE_HOME=%GRADLE_BASE%\gradle-%GRADLE_VERSION%"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base='%GRADLE_BASE%'; New-Item -ItemType Directory -Force -Path $base | Out-Null; $archive=Join-Path $base 'gradle-%GRADLE_VERSION%-bin.zip'; if (-not (Test-Path $archive)) { Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $archive }; Expand-Archive -LiteralPath $archive -DestinationPath $base -Force; Remove-Item -LiteralPath $archive -Force"
  if errorlevel 1 exit /b %ERRORLEVEL%
)
call "%GRADLE_HOME%\bin\gradle.bat" -p "%~dp0" %*
exit /b %ERRORLEVEL%
