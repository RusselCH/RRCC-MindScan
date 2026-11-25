@echo off
setlocal
cd /d "%~dp0"
title RRCC MindScan - Generar EXE

set "RRCC_PROYECTO=%~dp0rrcc-mindscan"
set "RRCC_SALIDA=%~dp0RRCC-EXE"
set "RRCC_TEMP=%~dp0RRCC-EXE\temporal"

if not exist "%RRCC_PROYECTO%\pom.xml" goto rrccSinProyecto
where java >nul 2>nul
if errorlevel 1 goto rrccSinJava
where jpackage >nul 2>nul
if not errorlevel 1 goto rrccHerramientasListas
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "PATH=%JAVA_HOME%\bin;%PATH%" & goto rrccHerramientasListas
for /d %%D in ("%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Microsoft\jdk*" "%ProgramFiles%\Zulu\zulu*" "%ProgramFiles%\Amazon Corretto\jdk*") do (
    if exist "%%~D\bin\jpackage.exe" set "PATH=%%~D\bin;%PATH%"
)
where jpackage >nul 2>nul
if errorlevel 1 goto rrccSinJpackage
:rrccHerramientasListas
where jlink >nul 2>nul
if errorlevel 1 goto rrccSinJpackage

echo.
echo [1/4] Compilando el codigo de rrcc-mindscan (la primera vez descarga librerias, puede tardar)...
pushd "%RRCC_PROYECTO%"
call "%RRCC_PROYECTO%\mvnw.cmd" -q -DskipTests -f "%RRCC_PROYECTO%\pom.xml" clean package
if errorlevel 1 (popd & goto rrccErrorMaven)
popd

echo [2/4] Preparando archivos fuera de la carpeta del codigo...
if exist "%RRCC_SALIDA%" rmdir /s /q "%RRCC_SALIDA%"
mkdir "%RRCC_TEMP%\entrada"
copy /y "%RRCC_PROYECTO%\target\rrcc-mindscan-1.0.0.jar" "%RRCC_TEMP%\entrada\" >nul
if errorlevel 1 goto rrccErrorMaven

echo [3/4] Creando un Java reducido para incluirlo dentro del programa...
jlink --add-modules java.se,jdk.unsupported,jdk.localedata,jdk.charsets,jdk.zipfs,jdk.management --strip-debug --no-header-files --no-man-pages --output "%RRCC_TEMP%\runtime"
if errorlevel 1 goto rrccErrorJlink

echo [4/4] Empaquetando RRCCMindScan.exe...
jpackage --type app-image --name RRCCMindScan --app-version 1.0.0 --vendor "RRCC" --description "Analizador de documentos sin inteligencia artificial" --input "%RRCC_TEMP%\entrada" --main-jar rrcc-mindscan-1.0.0.jar --runtime-image "%RRCC_TEMP%\runtime" --java-options "-Dfile.encoding=UTF-8" --dest "%RRCC_SALIDA%"
if errorlevel 1 goto rrccErrorJpackage

rmdir /s /q "%RRCC_TEMP%"
pushd "%RRCC_PROYECTO%"
call "%RRCC_PROYECTO%\mvnw.cmd" -q -f "%RRCC_PROYECTO%\pom.xml" clean
popd

echo.
echo ============================================================
echo  LISTO: RRCC-EXE\RRCCMindScan\RRCCMindScan.exe
echo  La carpeta rrcc-mindscan quedo solo con el codigo.
echo  Para llevarlo a otra PC copia TODA la carpeta
echo  RRCC-EXE\RRCCMindScan (no necesita Java instalado).
echo ============================================================
start "" "%RRCC_SALIDA%\RRCCMindScan"
pause
exit /b 0

:rrccSinProyecto
echo No se encontro la carpeta rrcc-mindscan junto a este archivo.
echo Este .bat debe estar al lado de la carpeta rrcc-mindscan, no dentro.
pause
exit /b 1

:rrccSinJava
echo No se encontro Java. Instala un JDK 17 o superior y vuelve a ejecutar este archivo.
pause
exit /b 1

:rrccSinJpackage
echo Se encontro Java, pero no jpackage ni jlink. Instala un JDK 17 o superior (no solo el JRE) y agrega su carpeta bin al PATH.
pause
exit /b 1

:rrccErrorMaven
echo Fallo la compilacion. Revisa los mensajes de arriba y que tengas conexion a internet la primera vez.
pause
exit /b 1

:rrccErrorJlink
echo Fallo jlink al crear el Java reducido.
pause
exit /b 1

:rrccErrorJpackage
echo Fallo jpackage al crear el .exe.
pause
exit /b 1
