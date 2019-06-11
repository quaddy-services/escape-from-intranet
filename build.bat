echo on
call mvn clean package
if %errorlevel% == 1 pause
if %errorlevel% == 1 goto end

echo on
for /F "delims== tokens=1,2" %%F in (target\maven-archiver\pom.properties) do set %%F=%%G

if x%NSIS_HOME%x == xx Echo Could not generate Windows Setup as NSIS_HOME is not set.
if x%NSIS_HOME%x == xx goto error_nsis


%NSIS_HOME%\makensis /DdisplayVersion=%version% src/main/nsis/windows-setup.nsi
if %errorlevel% == 1 goto error_nsis

Echo Finished.

goto end

rem prepare for signing
:error_sdk
echo off
echo install windows 10 SDK via https://visualstudio.microsoft.com/
echo and component Windows 10 SDK
echo and set WIN10_SDK_HOME=C:\Program Files (x86)\Windows Kits\10\bin\10.0.18362.0\x86
goto end

:error_nsis
echo off
Echo Error on NSIS. Maybe that helps:
Echo Download nsis-3.04-log.zip from https://sourceforge.net/projects/nsis/files/NSIS%203/3.04/
Echo Unzip it and refer it via set NSIS_HOME=C:\programs\nsis-3.04-log
goto end


:end