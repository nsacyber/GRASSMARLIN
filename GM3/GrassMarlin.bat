@ECHO OFF
IF [%1]==[-config] GOTO config
IF [%1]==[-help] GOTO help
IF [%1]==[--help] GOTO help
IF [%1]==[-?] GOTO help
IF [%1]==[/?] GOTO help


REM Launch GrassMarlin
IF "%1"=="" (
  SET halfmem=4096
  SET quartermem=2048
) ELSE (
  SET /A halfmem=%1/2
  SET /A quartermem=halfmem/2
)

java.exe -Dnio.mx=%quartermem%mb -Dnio.ms=512mb -server -d64 -Xms%halfmem%m -Xmx%halfmem%m  -XX:+UseG1GC -XX:+DisableExplicitGC -XX:NewSize=%quartermem%m -XX:MaxGCPauseMillis=2000 -jar "%~dp0\GrassMarlin.jar" -ui SDI
GOTO end

:config
java.exe -version
java.exe -cp "%~dp0GrassMarlin.jar" util.SetConfiguration "-dGrassMarlin\appData=%~2"
GOTO end

:help
ECHO Usage: %0 (-config {AppData}|[{AvailableMemory}])
ECHO If the -config option is not present, GrassMarlin will be launched.
ECHO When the -config option is present, this will set per-machine config values.
ECHO
ECHO {AppData} - The Path to the AppData folder where data will be stored on the
ECHO             local machine.  This is not the path for user data, but rather for
ECHO             system-level data.
ECHO {AvailableMemory} - The amount of total system memory, in megabytes. If not
ECHO                     specified, 8192 will be assumed.  The options for the
ECHO                     Java Garbage Collector will be tuned to use half the total
ECHO                     system memory.
ECHO
ECHO
GOTO end

:end
IF ERRORLEVEL 1 (
  PAUSE
)