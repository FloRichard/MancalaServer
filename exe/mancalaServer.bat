@echo off
set /p port=Enter port number:
echo %port%

java -jar mancalaServeur.jar %port%
pause