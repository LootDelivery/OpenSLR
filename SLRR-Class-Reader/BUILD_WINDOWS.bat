@echo off
setlocal
cd /d "%~dp0"

echo =============================================
echo  SLRR Class Reader - native WinForms build
echo =============================================
echo.

set "MSBUILD_EXE="
where msbuild >nul 2>nul
if not errorlevel 1 set "MSBUILD_EXE=msbuild"

if not defined MSBUILD_EXE if exist "%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe" (
  for /f "usebackq delims=" %%i in (`"%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe" -latest -requires Microsoft.Component.MSBuild -find MSBuild\**\Bin\MSBuild.exe`) do set "MSBUILD_EXE=%%i"
)

if not defined MSBUILD_EXE (
  echo ERROR: MSBuild was not found.
  echo Install Visual Studio 2019/2022 or Visual Studio Build Tools with the .NET desktop build tools.
  echo No Node.js, Electron, or NuGet packages are required by this project.
  pause
  exit /b 1
)

echo Building SLRR Class Reader for Windows x64...
"%MSBUILD_EXE%" "SLRRClassReader.sln" /t:Rebuild /p:Configuration=Release /p:Platform=x64 /m
if errorlevel 1 (
  echo.
  echo ERROR: Build failed. See the compiler messages above.
  pause
  exit /b 1
)

echo.
echo Build complete:
echo %CD%\bin\Release\SLRR Class Reader.exe
pause
