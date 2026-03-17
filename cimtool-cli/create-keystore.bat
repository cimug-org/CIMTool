@echo off
REM =============================================================================
REM create-keystore.bat
REM
REM Generates a self-signed certificate and keystore for signing
REM cimtool-cli.jar.
REM
REM Both the keystore and signing properties file are created at:
REM   %USERPROFILE%\.cimtool\cimtool-keystore.jks
REM   %USERPROFILE%\.cimtool\cimtool-signing.properties
REM
REM Prerequisites:
REM   - keytool must be on your PATH (comes with the JDK)
REM
REM Usage:
REM   create-keystore.bat
REM
REM After running this script:
REM   1. Edit %USERPROFILE%\.cimtool\cimtool-signing.properties
REM      and fill in the passwords you chose below
REM   2. Run: mvn clean package -Psign
REM =============================================================================

setlocal

REM --- Configuration -----------------------------------------------------------
REM All signing files live together in the .cimtool folder (outside the repo)
set CIMTOOL_DIR=%USERPROFILE%\.cimtool
set KEYSTORE_FILE=%CIMTOOL_DIR%\cimtool-keystore.jks
set PROPS_FILE=%CIMTOOL_DIR%\cimtool-signing.properties

REM Key alias - used in signing.alias
set ALIAS=cimtool-cli

REM Certificate validity in days (3650 = ~10 years, plenty of runway before CA cert)
set VALIDITY=3650

REM Key algorithm and size
set KEYALG=RSA
set KEYSIZE=2048

REM Signature algorithm
set SIGALG=SHA256withRSA
REM -----------------------------------------------------------------------------

echo =======================================================================
echo   CIMTool CLI -- Self-Signed Keystore Generator
echo   Keystore:   %KEYSTORE_FILE%
echo   Properties: %PROPS_FILE%
echo =======================================================================
echo.
echo This will generate a self-signed certificate for development/internal
echo use. When you obtain a CA-issued certificate, replace the keystore
echo and update cimtool-signing.properties - no pom.xml changes needed.
echo.

REM Create the .cimtool directory if it doesn't exist
if not exist "%CIMTOOL_DIR%" (
    mkdir "%CIMTOOL_DIR%"
    echo Created directory: %CIMTOOL_DIR%
    echo.
)

REM Warn if keystore already exists
if not exist "%KEYSTORE_FILE%" goto :keygen
echo WARNING: A keystore already exists at:
echo   %KEYSTORE_FILE%
echo.
set /p CONFIRM="Overwrite it? (Y/N): "
if /i "%CONFIRM%"=="Y" goto :overwrite
echo Aborted.
goto :eof
:overwrite
del "%KEYSTORE_FILE%"
echo.
:keygen

echo You will be prompted for:
echo   - A keystore password  (remember this - you will enter it in cimtool-signing.properties)
echo   - A key password       (can be the same as the keystore password)
echo   - Distinguished Name details (organisation, city, country, etc.)
echo.
echo Alias:     %ALIAS%
echo Validity:  %VALIDITY% days (~10 years)
echo Algorithm: %KEYALG% %KEYSIZE%-bit / %SIGALG%
echo.

keytool -genkeypair ^
    -keystore "%KEYSTORE_FILE%" ^
    -alias "%ALIAS%" ^
    -keyalg %KEYALG% ^
    -keysize %KEYSIZE% ^
    -sigalg %SIGALG% ^
    -validity %VALIDITY% ^
    -storetype JKS

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: keytool failed. Ensure the JDK bin directory is on your PATH.
    goto :eof
)

REM Copy the properties template to the .cimtool folder if not already present
if not exist "%PROPS_FILE%" (
    if exist "%~dp0cimtool-signing.properties.template" (
        copy "%~dp0cimtool-signing.properties.template" "%PROPS_FILE%" >nul
        echo.
        echo Copied signing properties template to:
        echo   %PROPS_FILE%
    )
)

echo.
echo =======================================================================
echo   Keystore created successfully.
echo.
echo   Keystore:   %KEYSTORE_FILE%
echo   Alias:      %ALIAS%
echo   Properties: %PROPS_FILE%
echo.
echo   Next steps:
echo     1. Open %PROPS_FILE%
echo     2. Set signing.storepass and signing.keypass to the
echo        passwords you just entered above
echo        (signing.keystore and signing.alias are pre-filled)
echo     3. Run: mvn clean package -Psign
echo =======================================================================

endlocal
