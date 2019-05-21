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

:error_nsis
echo off
Echo Error on NSIS. Maybe that helps:
Echo Download nsis-3.04-log.zip from https://sourceforge.net/projects/nsis/files/NSIS%203/3.04/
Echo Unzip it and refer it via NSIS_HOME
goto end


:end