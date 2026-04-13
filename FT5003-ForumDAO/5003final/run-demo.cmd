@echo off
setlocal

REM One-click demo runner for Windows PowerShell/CMD.
REM Prereqs: Hardhat node running at http://127.0.0.1:8545 and ForumTreasury deployed.

cd /d "%~dp0"

REM Prefer the portable JDK under .tools (created during setup in this repo)
for /d %%D in (".tools\jdk17\*") do (
  if exist "%%D\bin\java.exe" (
    set "JAVA_HOME=%%~fD"
    goto :jdk_found
  )
)

:jdk_found
if not defined JAVA_HOME (
  echo ERROR: JAVA_HOME not set and portable JDK not found under .tools\jdk17.
  echo Please install JDK 17+ or place one under .tools\jdk17.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Use a project-local Maven repo to avoid permission issues on some machines
set "LOCAL_M2=%CD%\.m2repo"

call mvnw.cmd -q -Dmaven.repo.local="%LOCAL_M2%" spring-boot:run

endlocal

