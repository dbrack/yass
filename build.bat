@echo off

set version=0.0.0
set /p version=Version [ MAJOR.MINOR.PATCH or 'enter' for %version% ]?:

set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_92

cmd /c C:\Users\guru\development\gradle-2.13\bin\gradle.bat -Pversion=%version%

cd ts
cmd /c npm install
cmd /c node_modules\.bin\tsc

pause
