@echo off

set base=%~dp0
cd /d %base%

java -cp "..\out\production\VolMonger\;..\lib;..\lib\*" com.groksoft.VolMonger