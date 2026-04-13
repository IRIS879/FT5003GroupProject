@echo off
setlocal

REM Cleans local build artifacts/deps (safe to delete; can be re-generated).
REM Run from: C:\Users\Try\Documents\GitHub\FT5003GroupProject\5003final

cd /d "%~dp0"

echo Cleaning local folders...
for %%D in ("target" "artifacts" "cache" "ignition" ".m2repo" "node_modules") do (
  if exist "%%~D" (
    echo - removing %%~D
    rmdir /s /q "%%~D"
  )
)

echo Done.
echo Note: .tools (portable JDK) is intentionally NOT deleted.
echo If you want to remove it too, delete .tools manually and install JDK 17+ system-wide.

endlocal

