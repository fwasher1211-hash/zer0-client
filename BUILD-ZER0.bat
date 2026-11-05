@echo off
title ZER0 CLIENT - build
cd /d "%~dp0"
echo.
echo  Building ZER0 CLIENT (first build downloads Minecraft + Fabric, ~5 minutes)...
echo.
call gradlew.bat build --stacktrace
if errorlevel 1 (
  echo.
  echo  BUILD FAILED - see README.md, section "If the build fails".
  echo  Tip: run tools\update-versions.bat once, then try again.
  pause
  exit /b 1
)
echo.
echo  Done!  Your mod:  build\dist\ZER0-CLIENT-*.jar
explorer build\dist
pause
