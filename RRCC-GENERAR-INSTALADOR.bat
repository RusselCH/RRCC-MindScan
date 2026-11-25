@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
echo ============================================================
echo  RRCC MindScan - Generar instalador (Inno Setup)
echo ============================================================

if not exist "RRCC-EXE\RRCCMindScan\RRCCMindScan.exe" (
    echo [1/3] No existe el .exe todavia. Generandolo con RRCC-GENERAR-EXE.bat...
    call "%~dp0RRCC-GENERAR-EXE.bat" < nul
    cd /d "%~dp0"
) else (
    echo [1/3] Se usara el .exe de RRCC-EXE\RRCCMindScan.
)
if not exist "RRCC-EXE\RRCCMindScan\RRCCMindScan.exe" (
    echo ERROR: no se pudo generar el .exe.
    goto :fin
)

echo [2/3] Buscando Inno Setup...
set "RRCC_ISCC="
for %%D in ("%ProgramFiles(x86)%\Inno Setup 6" "%ProgramFiles%\Inno Setup 6" "%LOCALAPPDATA%\Programs\Inno Setup 6") do (
    if exist "%%~D\ISCC.exe" set "RRCC_ISCC=%%~D\ISCC.exe"
)
if not defined RRCC_ISCC (
    echo Inno Setup no esta instalado. Instalandolo con winget...
    winget install -e --id JRSoftware.InnoSetup --scope user --silent --accept-package-agreements --accept-source-agreements
    for %%D in ("%ProgramFiles(x86)%\Inno Setup 6" "%ProgramFiles%\Inno Setup 6" "%LOCALAPPDATA%\Programs\Inno Setup 6") do (
        if exist "%%~D\ISCC.exe" set "RRCC_ISCC=%%~D\ISCC.exe"
    )
)
if not defined RRCC_ISCC (
    echo ERROR: no se encontro Inno Setup 6.
    echo Descargalo gratis de https://jrsoftware.org/isdl.php , instalalo y vuelve a ejecutar este archivo.
    goto :fin
)
echo Inno Setup: %RRCC_ISCC%

echo [3/3] Creando el instalador...
"%RRCC_ISCC%" /Q "RRCC-INSTALADOR.iss"
if errorlevel 1 (
    echo ERROR: Inno Setup no pudo crear el instalador.
    goto :fin
)
echo.
echo ============================================================
echo  LISTO: RRCC-INSTALADOR\RRCCMindScan-Instalador-v2.0.exe
echo  Ese unico archivo instala el programa con el asistente
echo  Siguiente - Siguiente - Instalar - Finalizar.
echo ============================================================
start "" "%~dp0RRCC-INSTALADOR"

:fin
endlocal
