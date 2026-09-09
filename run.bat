@echo off
chcp 65001 >nul
where java >nul 2>nul
if errorlevel 1 (
  echo ERRO: Java nao encontrado.
  echo Instale o JDK 21 e tente novamente.
  pause
  exit /b 1
)
where javac >nul 2>nul
if errorlevel 1 (
  echo ERRO: javac nao encontrado. Instale o JDK 21 completo.
  pause
  exit /b 1
)
if not exist out mkdir out
javac --add-modules jdk.httpserver -encoding UTF-8 -d out src\ConsumoLavadoraWeb.java
if errorlevel 1 (
  echo Falha na compilacao.
  pause
  exit /b 1
)
start "" http://localhost:8080
java --add-modules jdk.httpserver -cp out ConsumoLavadoraWeb
pause
