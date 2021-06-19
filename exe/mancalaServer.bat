@echo off
set /p port=Enter port number:

java -jar mancalaServeur.jar %port%
pause