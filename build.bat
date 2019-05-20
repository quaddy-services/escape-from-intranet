call mvn package
if %errorlevel% == 1 pause
if %errorlevel% == 1 goto end

echo on
for /F "delims== tokens=1,2" %%F in (target\maven-archiver\pom.properties) do set %%F=%%G

if x%NSIS_HOME%x == xx goto error_no_nsis

%NSIS_HOME%\makensis /DdisplayVersion=%version% src/main/nsis/windows-setup.nsi
if %errorlevel% == 1 pause
if %errorlevel% == 1 goto end

Echo Finished.

goto end

:error_no_nsis
echo off
Echo Could not generate Windows Setup as NSIS_HOME is not set.
Echo Download nsis-2.51.zip from https://sourceforge.net/projects/nsis/files/NSIS%202/2.51/
Echo Unzip it and refer it via NSIS_HOME
goto end


:end