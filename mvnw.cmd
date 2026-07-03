@echo off
setlocal
set MAVEN_VERSION=3.9.4
set SCRIPT_DIR=%~dp0
set MAVEN_DIR=%SCRIPT_DIR%.mvn\apache-maven-%MAVEN_VERSION%
IF NOT EXIST "%MAVEN_DIR%\bin\mvn.cmd" (
  echo Downloading Maven %MAVEN_VERSION%...
  set PSFILE=%SCRIPT_DIR%maven_download_%MAVEN_VERSION%.ps1
  > "%PSFILE%" echo $ErrorActionPreference = 'Stop'
  >> "%PSFILE%" echo $url = 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip'
  >> "%PSFILE%" echo $out = '%SCRIPT_DIR%maven.zip'
  >> "%PSFILE%" echo if ^( -not ^(Test-Path '%SCRIPT_DIR%.mvn'^) ^) { New-Item -ItemType Directory -Path '%SCRIPT_DIR%.mvn' ^| Out-Null }
  >> "%PSFILE%" echo Invoke-WebRequest -Uri $url -OutFile $out
  >> "%PSFILE%" echo Expand-Archive -LiteralPath $out -DestinationPath '%SCRIPT_DIR%.mvn' -Force
  >> "%PSFILE%" echo Remove-Item $out -Force
  powershell -NoProfile -ExecutionPolicy Bypass -File "%PSFILE%"
  if exist "%PSFILE%" del "%PSFILE%"
)
"%MAVEN_DIR%\bin\mvn.cmd" %*
