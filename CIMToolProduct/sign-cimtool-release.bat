@echo off
setlocal enabledelayedexpansion

REM Resolve the directory this script lives in (CIMToolProduct/)
set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR:~-1%"=="\" set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

REM cimtool-cli is a sibling directory of CIMToolProduct under the repo root
set CIMTOOL_CLI_DIR=%SCRIPT_DIR%\..\cimtool-cli

REM =============================================================================
REM  sign-cimtool-release.bat
REM
REM  Orchestrates the complete CIMTool release signing and packaging pipeline:
REM
REM    1. Signs all JAR files in the CIMToolProduct PDE export plugins\ directory
REM    2. Signs the CIMTool.exe Windows launcher
REM    3. Populates cimtool-cli lib-repo\ via install-jars.bat
REM    4. Builds the cimtool-cli uber JAR via Maven
REM    5. Signs cimtool-cli.jar
REM    6. Deploys cimtool-cli.jar to dist\ and generates its SHA-256 checksum
REM    7. Packages the signed CIMToolProduct export into a distribution ZIP
REM    8. Generates the ZIP SHA-256 checksum
REM
REM  On completion four artifacts are ready for upload to GitHub Releases:
REM    - CIMTool-<version>-win32.win32.x86_64.zip
REM    - CIMTool-<version>-win32.win32.x86_64.zip.sha256
REM    - cimtool-cli.jar
REM    - cimtool-cli.jar.sha256
REM
REM  Prerequisites
REM  -------------
REM  1. Eclipse Temurin JDK 20 must be installed from https://adoptium.net.
REM     Temurin includes the SunPKCS11 provider required by jarsigner to
REM     communicate with the IdenTrust EV hardware token. Other OpenJDK
REM     distributions such as Zulu omit this provider and cannot be used
REM     for JAR signing.
REM
REM  2. Windows SDK signtool.exe must be installed (via Visual Studio or a
REM     standalone Windows SDK download). Update the SIGNTOOL variable below
REM     if your installation path differs from the default.
REM
REM  3. SafeNet Authentication Client (SAC) must be installed and the
REM     IdenTrust EV USB token must be inserted before running this script.
REM
REM  4. Maven 3.9 or later must be installed and mvn must be on the PATH.
REM     Required to build the cimtool-cli uber JAR.
REM
REM  5. Review and update the USER CONFIGURATION section below before
REM     running for the first time.
REM
REM  Usage
REM  -----
REM  sign-cimtool-release.bat <export-root> <version-string>
REM
REM  Example:
REM  sign-cimtool-release.bat D:\CIMTool-Releases CIMTool-2.3.0-RC8
REM =============================================================================

REM =============================================================================
REM  USER CONFIGURATION
REM  Review and update these variables before running for the first time.
REM =============================================================================

REM Full path to the Eclipse Temurin JDK 20 jarsigner executable.
REM Default install location for OpenJDK20U-jdk_x64_windows_hotspot_20.0.2_9:
set JARSIGNER="C:\Program Files\Eclipse Adoptium\jdk-20.0.2.9-hotspot\bin\jarsigner.exe"

REM Full path to signtool.exe from the Windows SDK.
REM Adjust if your Windows SDK version or install path differs.
set SIGNTOOL="C:\Program Files (x86)\Windows Kits\10\bin\10.0.26100.0\x64\signtool.exe"

REM Maven executable. Must be on the PATH. Override here if needed.
set MVN=mvn

REM Certificate alias as shown in SafeNet Authentication Client Tools
REM under: Tokens > IdenTrust Token > User certificates
set CERT_ALIAS=2a5b28ed-47ef-2c08-a705-896ee5b3016e

REM Organization name exactly as it appears in the EV certificate "Issued to" field.
set CERT_SUBJECT=UCA USERS GROUP

REM Primary PKCS#11 library installed by SafeNet Authentication Client.
REM If not found the script will attempt the alternate pkcs11.dll location.
set PKCS11_DLL=C:\Windows\System32\eTPKCS11.dll

REM IdenTrust RFC 3161 timestamp authority URL.
set TSA_URL=http://timestamp.identrust.com

REM Space-separated list of JAR filenames (without path) to skip during signing.
REM These are JARs that cannot be signed due to structural defects in the JAR
REM itself (e.g. duplicate ZIP entries) that are outside of CIMTool's control.
REM Skipped JARs are still protected by the distribution ZIP SHA-256 checksum.
REM
REM asp-server-asciidoctorj-dist.jar: part of de.jcup.asciidoctoreditor; contains
REM   a duplicate META-INF/BSDL entry that prevents jarsigner from processing it.
set SKIP_JARS=asp-server-asciidoctorj-dist.jar

REM =============================================================================
REM  END OF USER CONFIGURATION
REM =============================================================================

echo.
echo =============================================================================
echo  CIMTool Release Signing and Packaging
echo =============================================================================
echo.

REM --- Validate parameters ---
set EXPORT_ROOT=%~1
set VERSION=%~2

if "%EXPORT_ROOT%"=="" (
    echo ERROR: Export root directory not specified.
    echo.
    echo Usage:   sign-cimtool-release.bat ^<export-root^> ^<version-string^>
    echo Example: sign-cimtool-release.bat D:\CIMTool-Releases CIMTool-2.3.0-RC8
    exit /b 1
)

if "%VERSION%"=="" (
    echo ERROR: Version string not specified.
    echo.
    echo Usage:   sign-cimtool-release.bat ^<export-root^> ^<version-string^>
    echo Example: sign-cimtool-release.bat D:\CIMTool-Releases CIMTool-2.3.0-RC8
    exit /b 1
)

set PRODUCT_DIR=%EXPORT_ROOT%\%VERSION%
set PLUGINS_DIR=%PRODUCT_DIR%\plugins

REM --- Validate export directory ---
if not exist "%PRODUCT_DIR%\" (
    echo ERROR: Product directory not found: %PRODUCT_DIR%
    echo        Verify the export root and version string are correct.
    exit /b 1
)

if not exist "%PLUGINS_DIR%\" (
    echo ERROR: Plugins directory not found: %PLUGINS_DIR%
    echo        Verify that the Eclipse PDE product export completed successfully.
    exit /b 1
)

REM --- Validate jarsigner ---
if not exist %JARSIGNER% (
    echo ERROR: jarsigner not found at %JARSIGNER%
    echo        Install Eclipse Temurin JDK 20 from https://adoptium.net
    echo        or update the JARSIGNER variable in this script.
    exit /b 1
)

REM --- Validate signtool ---
if not exist %SIGNTOOL% (
    echo ERROR: signtool.exe not found at %SIGNTOOL%
    echo        Install the Windows SDK or update the SIGNTOOL variable in this script.
    exit /b 1
)

REM --- Validate PKCS#11 DLL ---
if not exist "%PKCS11_DLL%" (
    echo WARNING: Primary PKCS#11 library not found at %PKCS11_DLL%
    echo          Trying alternate: C:\Windows\System32\pkcs11.dll
    set PKCS11_DLL=C:\Windows\System32\pkcs11.dll
    if not exist "!PKCS11_DLL!" (
        echo ERROR: No PKCS#11 library found.
        echo        Ensure SafeNet Authentication Client ^(SAC^) is installed.
        exit /b 1
    )
    echo          Found alternate PKCS#11 library: !PKCS11_DLL!
)

REM --- Validate Maven ---
where %MVN% >nul 2>&1
if errorlevel 1 (
    echo ERROR: Maven ^(%MVN%^) not found on PATH.
    echo        Install Maven 3.9 or later and ensure mvn is on the PATH.
    exit /b 1
)

REM --- Validate cimtool-cli directory ---
if not exist "%CIMTOOL_CLI_DIR%\pom.xml" (
    echo ERROR: cimtool-cli project not found at %CIMTOOL_CLI_DIR%
    echo        Ensure this script is run from within the CIMToolProduct directory
    echo        and that cimtool-cli is a sibling directory under the repo root.
    exit /b 1
)

REM --- Write temporary PKCS#11 config ---
set PKCS11_CFG=%TEMP%\cimtool-pkcs11.cfg
echo name = IdenTrust-EV> "%PKCS11_CFG%"
echo library = %PKCS11_DLL%>> "%PKCS11_CFG%"

REM --- Confirm token is inserted ---
echo.
echo IMPORTANT: Ensure the IdenTrust EV USB token is inserted before continuing.
echo.
set /p TOKEN_CONFIRM="Is the IdenTrust EV USB token inserted? (Y/N): "
if /i not "%TOKEN_CONFIRM%"=="Y" (
    echo.
    echo Aborted. Please insert the token and re-run the script.
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)
echo.

REM --- Prompt for token PIN once (input is masked) ---
set PS_GETPIN=%TEMP%\cimtool-getpin.ps1
echo $s = Read-Host 'Token PIN' -AsSecureString > "%PS_GETPIN%"
echo [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)) >> "%PS_GETPIN%"
echo Please enter your IdenTrust token PIN:
for /f "usebackq delims=" %%P in (`powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_GETPIN%"`) do set TOKEN_PIN=%%P
del "%PS_GETPIN%" 2>nul
echo.

REM --- Step 1: Sign all JAR files ---
echo [Step 1/8] Signing JAR files...
echo           Source: %PLUGINS_DIR%
echo.

set JAR_COUNT=0
set JAR_SKIPPED=0
set JAR_ERRORS=0

for /r "%PLUGINS_DIR%" %%F in (*.jar) do (
    set JAR_NAME=%%~nxF
    set JAR_SKIP=0
    for %%S in (%SKIP_JARS%) do (
        if /i "!JAR_NAME!"=="%%S" set JAR_SKIP=1
    )
    if "!JAR_SKIP!"=="1" (
        echo   Skipping: !JAR_NAME! ^(known unsignable JAR - see SKIP_JARS^)
        set /a JAR_SKIPPED+=1
    ) else (
        set /a JAR_COUNT+=1
        echo   Signing: !JAR_NAME!
        %JARSIGNER% -providerClass sun.security.pkcs11.SunPKCS11 ^
                    -providerArg "%PKCS11_CFG%" ^
                    -keystore NONE ^
                    -storetype PKCS11 ^
                    -storepass "!TOKEN_PIN!" ^
                    -tsa %TSA_URL% ^
                    -digestalg SHA-256 ^
                    -sigalg SHA256withRSA ^
                    "%%F" "%CERT_ALIAS%"
        if errorlevel 1 (
            echo   ERROR: Failed to sign !JAR_NAME!
            set /a JAR_ERRORS+=1
        )
    )
)

echo.
echo   Signed !JAR_COUNT! JAR(s), skipped !JAR_SKIPPED! JAR(s), !JAR_ERRORS! error(s).

if !JAR_ERRORS! GTR 0 (
    echo.
    echo ERROR: JAR signing completed with errors. Aborting.
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

REM --- Step 2: Sign CIMTool.exe ---
echo.
echo [Step 2/8] Signing CIMTool.exe...

if not exist "%PRODUCT_DIR%\CIMTool.exe" (
    echo ERROR: CIMTool.exe not found at %PRODUCT_DIR%\CIMTool.exe
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

%SIGNTOOL% sign /tr %TSA_URL% /td sha256 /fd sha256 /n "%CERT_SUBJECT%" "%PRODUCT_DIR%\CIMTool.exe"
if errorlevel 1 (
    echo ERROR: Failed to sign CIMTool.exe
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

REM --- Step 3: Populate cimtool-cli lib-repo ---
echo.
echo [Step 3/8] Populating cimtool-cli lib-repo via install-jars.bat...
echo           Export root: %PRODUCT_DIR%
echo.

call "%CIMTOOL_CLI_DIR%\install-jars.bat" "%PRODUCT_DIR%"
if errorlevel 1 (
    echo ERROR: install-jars.bat failed.
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

REM --- Step 4: Build cimtool-cli uber JAR ---
echo.
echo [Step 4/8] Building cimtool-cli uber JAR...

pushd "%CIMTOOL_CLI_DIR%"
call %MVN% clean package -q
if errorlevel 1 (
    echo ERROR: Maven build failed.
    popd
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)
popd

set CLI_JAR=%CIMTOOL_CLI_DIR%\target\cimtool-cli.jar
if not exist "%CLI_JAR%" (
    echo ERROR: Expected uber JAR not found at %CLI_JAR%
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)
echo   OK: %CLI_JAR%

REM --- Step 5: Sign cimtool-cli.jar ---
echo.
echo [Step 5/8] Signing cimtool-cli.jar...

%JARSIGNER% -providerClass sun.security.pkcs11.SunPKCS11 ^
            -providerArg "%PKCS11_CFG%" ^
            -keystore NONE ^
            -storetype PKCS11 ^
            -storepass "!TOKEN_PIN!" ^
            -tsa %TSA_URL% ^
            -digestalg SHA-256 ^
            -sigalg SHA256withRSA ^
            "%CLI_JAR%" "%CERT_ALIAS%"
if errorlevel 1 (
    echo ERROR: Failed to sign cimtool-cli.jar
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

REM --- Step 6: Deploy cimtool-cli.jar to dist\ and generate SHA-256 ---
set CLI_DIST_DIR=%CIMTOOL_CLI_DIR%\dist
set CLI_DIST_JAR=%CLI_DIST_DIR%\cimtool-cli.jar
set CLI_SHA256=%CLI_DIST_DIR%\cimtool-cli.jar.sha256

echo.
echo [Step 6/8] Deploying to dist\ and generating SHA-256 checksum...
echo           %CLI_DIST_JAR%

if not exist "%CLI_DIST_DIR%\" mkdir "%CLI_DIST_DIR%"

copy /y "%CLI_JAR%" "%CLI_DIST_JAR%" >nul
if errorlevel 1 (
    echo ERROR: Failed to copy cimtool-cli.jar to dist\
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

powershell -NoProfile -Command "(Get-FileHash '%CLI_DIST_JAR%' -Algorithm SHA256).Hash.ToLower() + '  cimtool-cli.jar' | Out-File -FilePath '%CLI_SHA256%' -Encoding ascii -NoNewline"
if errorlevel 1 (
    echo ERROR: Failed to generate cimtool-cli.jar SHA-256 checksum.
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)
echo           %CLI_SHA256%

REM --- Step 7: Package ZIP ---
set ZIP_NAME=%VERSION%-win32.win32.x86_64.zip
set ZIP_PATH=%EXPORT_ROOT%\%ZIP_NAME%

echo.
echo [Step 7/8] Creating distribution archive...
echo           %ZIP_PATH%

powershell -NoProfile -Command "Compress-Archive -Path '%PRODUCT_DIR%' -DestinationPath '%ZIP_PATH%' -Force"
if errorlevel 1 (
    echo ERROR: Failed to create ZIP archive.
    set TOKEN_PIN=
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

REM --- Step 8: Generate ZIP SHA-256 checksum ---
set SHA256_PATH=%EXPORT_ROOT%\%ZIP_NAME%.sha256

echo.
echo [Step 8/8] Generating ZIP SHA-256 checksum...
echo           %SHA256_PATH%

powershell -NoProfile -Command "(Get-FileHash '%ZIP_PATH%' -Algorithm SHA256).Hash.ToLower() + '  %ZIP_NAME%' | Out-File -FilePath '%SHA256_PATH%' -Encoding ascii -NoNewline"
if errorlevel 1 (
    echo ERROR: Failed to generate ZIP SHA-256 checksum.
    del "%PKCS11_CFG%" 2>nul
    exit /b 1
)

REM --- Cleanup ---
set TOKEN_PIN=
del "%PKCS11_CFG%" 2>nul
del "%PS_GETPIN%" 2>nul

REM --- Summary ---
echo.
echo =============================================================================
echo  Signing and packaging complete.
echo.
echo  CIMToolProduct artifacts:
echo    Archive:   %ZIP_PATH%
echo    Checksum:  %SHA256_PATH%
echo.
echo  cimtool-cli artifacts:
echo    JAR:       %CLI_DIST_JAR%
echo    Checksum:  %CLI_SHA256%
echo.
echo  Upload all four files to the GitHub release at:
echo  https://github.com/cimug-org/CIMTool/releases
echo =============================================================================
echo.

endlocal
exit /b 0
