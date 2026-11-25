@echo off
rem Rebuild slrr_host and run it again: the game locks the exe, so it is closed first.
cd /d "%~dp0"
taskkill /IM slrr_host.exe /F >nul 2>&1
echo Building slrr_host ...
cmake --build "%~dp0openslrr\build" --config Release --target slrr_host
if errorlevel 1 (
  echo.
  echo BUILD FAILED -- see the errors above.
  pause
  exit /b 1
)
echo.
echo Done. Start the game with OpenSLRR.bat.
pause
