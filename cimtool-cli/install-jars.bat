@echo off
REM =============================================================================
REM install-jars.bat
REM
REM Installs kena.jar and cimutil.jar from a CIMTool Eclipse PDE product export,
REM plus all vendored JARs from Kena\lib\ and CIMUtil\lib\, into the local Maven
REM file repository at cimtool-cli\lib-repo\.
REM
REM kena.jar and cimutil.jar are located by searching the plugins\ directory of
REM the PDE export for plugin folders matching au.com.langdale.kena_* and
REM au.com.langdale.cimutil_*. The version number is extracted from the plugin
REM folder name (e.g. au.com.langdale.kena_3.3.4 -> 3.3.4) rather than from
REM the JAR filename, since the JARs are named kena.jar and cimutil.jar without
REM a version suffix.
REM
REM NOTE: The version extracted from the plugin folder name must match the
REM <version> declared for kena and cimutil in pom.xml. Update pom.xml if
REM the version changes.
REM
REM Usage:
REM   install-jars.bat <export-root>
REM
REM   <export-root>  Path to the root of the Eclipse PDE product export.
REM                  If omitted, the script will prompt for it interactively.
REM
REM Examples:
REM   install-jars.bat D:\CIMTool-Releases\CIMTool-2.3.0
REM   install-jars.bat "D:\My Releases\CIMTool-2.3.0"
REM
REM Prerequisites:
REM   - mvn must be on your PATH
REM   - A CIMTool Eclipse PDE product export must exist at <export-root>
REM     with a plugins\ subdirectory containing au.com.langdale.kena_* and
REM     au.com.langdale.cimutil_* plugin folders
REM =============================================================================

setlocal

set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR:~-1%"=="\" set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

set REPO=%SCRIPT_DIR%\lib-repo
set KENA_LIB=%SCRIPT_DIR%\..\Kena\lib
set CIMUTIL_LIB=%SCRIPT_DIR%\..\CIMUtil\lib

REM -----------------------------------------------------------------------
REM Resolve the export root — from argument or interactive prompt
REM -----------------------------------------------------------------------
if "%~1"=="" (
    echo.
    echo   No export root specified.
    echo   Enter the path to the CIMTool Eclipse PDE product export root.
    echo   Example: D:\CIMTool-Releases\CIMTool-2.3.0
    echo   Tip: If the path contains spaces, wrap it in double quotes.
    echo.
    set /p EXPORT_ROOT="  Export root: "
) else (
    set EXPORT_ROOT=%~1
)

REM Strip any surrounding quotes that may have been passed
set EXPORT_ROOT=%EXPORT_ROOT:"=%

echo.
echo =======================================================================
echo   CIMTool CLI -- Local Repository Installer
echo   Repository:   %REPO%
echo   Export root:  %EXPORT_ROOT%
echo =======================================================================
echo.

REM Validate the export root exists
if not exist "%EXPORT_ROOT%" (
    echo [ERROR] Export root not found: %EXPORT_ROOT%
    echo         Check the path and try again.
    goto :end
)

set PLUGINS_DIR=%EXPORT_ROOT%\plugins
if not exist "%PLUGINS_DIR%" (
    echo [ERROR] plugins\ directory not found under: %EXPORT_ROOT%
    echo         Ensure this is the root of a CIMTool PDE product export.
    goto :end
)

REM -----------------------------------------------------------------------
REM Purge stale kena and cimutil entries from lib-repo/ AND from the main
REM local Maven cache (~/.m2). Both purges are unconditional — they run
REM before the PDE export JARs are searched so that even if the search
REM fails (MISSING), Maven cannot fall back to a stale cached copy.
REM
REM lib-repo/ purge: prevents version accumulation when the version number
REM changes between runs (e.g. kena 3.3.4 -> 3.3.5).
REM
REM .m2 purge: prevents Maven from resolving a previously-cached JAR that
REM may contain ECJ stub classes from an earlier failed Eclipse build.
REM Vendored lib JARs are not purged here as their versions are fixed.
REM -----------------------------------------------------------------------
if exist "%REPO%\au\com\langdale\kena" (
    rmdir /s /q "%REPO%\au\com\langdale\kena"
)
if exist "%REPO%\au\com\langdale\cimutil" (
    rmdir /s /q "%REPO%\au\com\langdale\cimutil"
)
if exist "%USERPROFILE%\.m2\repository\au\com\langdale\kena" (
    rmdir /s /q "%USERPROFILE%\.m2\repository\au\com\langdale\kena"
)
if exist "%USERPROFILE%\.m2\repository\au\com\langdale\cimutil" (
    rmdir /s /q "%USERPROFILE%\.m2\repository\au\com\langdale\cimutil"
)

REM -----------------------------------------------------------------------
REM CIMTool core modules — sourced from the PDE product export plugins\
REM
REM The for /d loop searches plugins\ for the versioned plugin folder
REM matching the wildcard pattern. The version is extracted from the folder
REM name (e.g. au.com.langdale.kena_3.3.4 -> 3.3.4) since the JARs are
REM named kena.jar and cimutil.jar without a version suffix.
REM -----------------------------------------------------------------------
echo --- CIMTool core modules -------------------------------------------
echo.

echo   Installing kena
set KENA_JAR=
set KENA_FOLDER=
for /d %%D in ("%PLUGINS_DIR%\au.com.langdale.kena_*") do (
    if exist "%%D\kena.jar" (
        set KENA_JAR=%%D\kena.jar
        set KENA_FOLDER=%%~nxD
    )
)
if "%KENA_JAR%"=="" (
    echo   [MISSING] No au.com.langdale.kena_* folder containing kena.jar found under %PLUGINS_DIR%
    echo             Ensure the PDE export completed successfully.
    goto :kena_done
)
echo   Found: %KENA_JAR%
REM Extract version from folder name: au.com.langdale.kena_3.3.4 -> 3.3.4
set KENA_VERSION=%KENA_FOLDER:au.com.langdale.kena_=%
echo   Version: %KENA_VERSION%
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="au.com.langdale" -DartifactId="kena" -Dversion="%KENA_VERSION%" -Dfile="%KENA_JAR%" --quiet
echo   OK
:kena_done
echo.

echo   Installing cimutil
set CIMUTIL_JAR=
set CIMUTIL_FOLDER=
for /d %%D in ("%PLUGINS_DIR%\au.com.langdale.cimutil_*") do (
    if exist "%%D\cimutil.jar" (
        set CIMUTIL_JAR=%%D\cimutil.jar
        set CIMUTIL_FOLDER=%%~nxD
    )
)
if "%CIMUTIL_JAR%"=="" (
    echo   [MISSING] No au.com.langdale.cimutil_* folder containing cimutil.jar found under %PLUGINS_DIR%
    echo             Ensure the PDE export completed successfully.
    goto :cimutil_done
)
echo   Found: %CIMUTIL_JAR%
REM Extract version from folder name: au.com.langdale.cimutil_2.3.0 -> 2.3.0
set CIMUTIL_VERSION=%CIMUTIL_FOLDER:au.com.langdale.cimutil_=%
echo   Version: %CIMUTIL_VERSION%
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="au.com.langdale" -DartifactId="cimutil" -Dversion="%CIMUTIL_VERSION%" -Dfile="%CIMUTIL_JAR%" --quiet
echo   OK
:cimutil_done
echo.

REM -----------------------------------------------------------------------
REM Prompt user before installing vendor JARs
REM
REM Vendor JARs (Kena\lib and CIMUtil\lib) rarely change between releases.
REM Re-installing them is only necessary if new JARs have been added or
REM existing ones updated since the last time the script was run.
REM -----------------------------------------------------------------------
echo.
echo   The vendor JARs (Kena\lib and CIMUtil\lib) rarely change between
echo   releases. Re-installing them is only needed if new or updated JARs
echo   have been added to those directories since the last run.
echo   If unsure, answer Y to ensure lib-repo is fully up to date.
echo.
set /p INSTALL_VENDOR="  Install vendor JARs? (Y/N): "
if /i not "%INSTALL_VENDOR%"=="Y" goto :skip_vendor

REM -----------------------------------------------------------------------
REM Kena\lib
REM Note: jena-2.6.3-tests.jar, iri-0.8-sources.jar, junit-4.5.jar, and
REM       slf4j-api-1.5.8.jar are intentionally omitted. See pom.xml.
REM -----------------------------------------------------------------------
echo --- Kena\lib -------------------------------------------------------
echo.

echo   Installing jena:2.6.3
if not exist "%KENA_LIB%\jena-2.6.3.jar" (
    echo   [MISSING] %KENA_LIB%\jena-2.6.3.jar -- skipping
    goto :jena_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="com.hp.hpl.jena" -DartifactId="jena" -Dversion="2.6.3" -Dfile="%KENA_LIB%\jena-2.6.3.jar" --quiet
echo   OK
:jena_done
echo.

echo   Installing arq:2.8.4
if not exist "%KENA_LIB%\arq-2.8.4.jar" (
    echo   [MISSING] %KENA_LIB%\arq-2.8.4.jar -- skipping
    goto :arq_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="com.hp.hpl.jena" -DartifactId="arq" -Dversion="2.8.4" -Dfile="%KENA_LIB%\arq-2.8.4.jar" --quiet
echo   OK
:arq_done
echo.

echo   Installing iri:0.8
if not exist "%KENA_LIB%\iri-0.8.jar" (
    echo   [MISSING] %KENA_LIB%\iri-0.8.jar -- skipping
    goto :iri_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="com.hp.hpl.jena" -DartifactId="iri" -Dversion="0.8" -Dfile="%KENA_LIB%\iri-0.8.jar" --quiet
echo   OK
:iri_done
echo.

echo   Installing lucene-core:2.3.1
if not exist "%KENA_LIB%\lucene-core-2.3.1.jar" (
    echo   [MISSING] %KENA_LIB%\lucene-core-2.3.1.jar -- skipping
    goto :lucene_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.apache.lucene" -DartifactId="lucene-core" -Dversion="2.3.1" -Dfile="%KENA_LIB%\lucene-core-2.3.1.jar" --quiet
echo   OK
:lucene_done
echo.

echo   Installing stax-api:1.0.1
if not exist "%KENA_LIB%\stax-api-1.0.1.jar" (
    echo   [MISSING] %KENA_LIB%\stax-api-1.0.1.jar -- skipping
    goto :stax_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="javax.xml.stream" -DartifactId="stax-api" -Dversion="1.0.1" -Dfile="%KENA_LIB%\stax-api-1.0.1.jar" --quiet
echo   OK
:stax_done
echo.

echo   Installing wstx-asl:3.2.9
if not exist "%KENA_LIB%\wstx-asl-3.2.9.jar" (
    echo   [MISSING] %KENA_LIB%\wstx-asl-3.2.9.jar -- skipping
    goto :wstx_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.codehaus.woodstox" -DartifactId="wstx-asl" -Dversion="3.2.9" -Dfile="%KENA_LIB%\wstx-asl-3.2.9.jar" --quiet
echo   OK
:wstx_done
echo.

echo   Installing xercesImpl:2.7.1
if not exist "%KENA_LIB%\xercesImpl-2.7.1.jar" (
    echo   [MISSING] %KENA_LIB%\xercesImpl-2.7.1.jar -- skipping
    goto :xerces_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="xerces" -DartifactId="xercesImpl" -Dversion="2.7.1" -Dfile="%KENA_LIB%\xercesImpl-2.7.1.jar" --quiet
echo   OK
:xerces_done
echo.

echo   Installing icu4j:71.1
if not exist "%KENA_LIB%\icu4j-71.1.jar" (
    echo   [MISSING] %KENA_LIB%\icu4j-71.1.jar -- skipping
    goto :icu4j_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="com.ibm.icu" -DartifactId="icu4j" -Dversion="71.1" -Dfile="%KENA_LIB%\icu4j-71.1.jar" --quiet
echo   OK
:icu4j_done
echo.

echo   Installing log4j:1.2.13
if not exist "%KENA_LIB%\log4j-1.2.13.jar" (
    echo   [MISSING] %KENA_LIB%\log4j-1.2.13.jar -- skipping
    goto :log4j_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="log4j" -DartifactId="log4j" -Dversion="1.2.13" -Dfile="%KENA_LIB%\log4j-1.2.13.jar" --quiet
echo   OK
:log4j_done
echo.

echo   Installing slf4j-log4j12:1.5.8
if not exist "%KENA_LIB%\slf4j-log4j12-1.5.8.jar" (
    echo   [MISSING] %KENA_LIB%\slf4j-log4j12-1.5.8.jar -- skipping
    goto :slf4j_log4j12_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.slf4j" -DartifactId="slf4j-log4j12" -Dversion="1.5.8" -Dfile="%KENA_LIB%\slf4j-log4j12-1.5.8.jar" --quiet
echo   OK
:slf4j_log4j12_done
echo.

REM -----------------------------------------------------------------------
REM CIMUtil\lib
REM -----------------------------------------------------------------------
echo --- CIMUtil\lib ----------------------------------------------------
echo.

echo   Installing Saxon-HE:10.8
if not exist "%CIMUTIL_LIB%\saxon-he-10.8.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\saxon-he-10.8.jar -- skipping
    goto :saxon_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="net.sf.saxon" -DartifactId="Saxon-HE" -Dversion="10.8" -Dfile="%CIMUTIL_LIB%\saxon-he-10.8.jar" --quiet
echo   OK
:saxon_done
echo.

echo   Installing easy-rules-core:4.1.0
if not exist "%CIMUTIL_LIB%\easy-rules-core-4.1.0.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\easy-rules-core-4.1.0.jar -- skipping
    goto :easyrules_core_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.jeasy" -DartifactId="easy-rules-core" -Dversion="4.1.0" -Dfile="%CIMUTIL_LIB%\easy-rules-core-4.1.0.jar" --quiet
echo   OK
:easyrules_core_done
echo.

echo   Installing easy-rules-support:4.1.0
if not exist "%CIMUTIL_LIB%\easy-rules-support-4.1.0.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\easy-rules-support-4.1.0.jar -- skipping
    goto :easyrules_support_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.jeasy" -DartifactId="easy-rules-support" -Dversion="4.1.0" -Dfile="%CIMUTIL_LIB%\easy-rules-support-4.1.0.jar" --quiet
echo   OK
:easyrules_support_done
echo.

echo   Installing poi:3.9
if not exist "%CIMUTIL_LIB%\poi-3.9.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\poi-3.9.jar -- skipping
    goto :poi_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.apache.poi" -DartifactId="poi" -Dversion="3.9" -Dfile="%CIMUTIL_LIB%\poi-3.9.jar" --quiet
echo   OK
:poi_done
echo.

echo   Installing commons-lang:2.6
if not exist "%CIMUTIL_LIB%\commons-lang-2.6.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\commons-lang-2.6.jar -- skipping
    goto :commons_lang_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="commons-lang" -DartifactId="commons-lang" -Dversion="2.6" -Dfile="%CIMUTIL_LIB%\commons-lang-2.6.jar" --quiet
echo   OK
:commons_lang_done
echo.

echo   Installing commons-logging:1.1.3
if not exist "%CIMUTIL_LIB%\commons-logging-1.1.3.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\commons-logging-1.1.3.jar -- skipping
    goto :commons_logging_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="commons-logging" -DartifactId="commons-logging" -Dversion="1.1.3" -Dfile="%CIMUTIL_LIB%\commons-logging-1.1.3.jar" --quiet
echo   OK
:commons_logging_done
echo.

echo   Installing xml-resolver:1.2
if not exist "%CIMUTIL_LIB%\xml-resolver-1.2.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\xml-resolver-1.2.jar -- skipping
    goto :xml_resolver_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="xml-resolver" -DartifactId="xml-resolver" -Dversion="1.2" -Dfile="%CIMUTIL_LIB%\xml-resolver-1.2.jar" --quiet
echo   OK
:xml_resolver_done
echo.

echo   Installing sqlite-jdbc:3.45.2.0
if not exist "%CIMUTIL_LIB%\sqlite-jdbc-3.45.2.0.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\sqlite-jdbc-3.45.2.0.jar -- skipping
    goto :sqlite_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.xerial" -DartifactId="sqlite-jdbc" -Dversion="3.45.2.0" -Dfile="%CIMUTIL_LIB%\sqlite-jdbc-3.45.2.0.jar" --quiet
echo   OK
:sqlite_done
echo.

echo   Installing slf4j-api:1.7.36
if not exist "%CIMUTIL_LIB%\slf4j-api-1.7.36.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\slf4j-api-1.7.36.jar -- skipping
    goto :slf4j_api_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.slf4j" -DartifactId="slf4j-api" -Dversion="1.7.36" -Dfile="%CIMUTIL_LIB%\slf4j-api-1.7.36.jar" --quiet
echo   OK
:slf4j_api_done
echo.

echo   Installing jackcess:2.2.3
if not exist "%CIMUTIL_LIB%\jackcess-2.2.3.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\jackcess-2.2.3.jar -- skipping
    goto :jackcess_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="com.healthmarketscience.jackcess" -DartifactId="jackcess" -Dversion="2.2.3" -Dfile="%CIMUTIL_LIB%\jackcess-2.2.3.jar" --quiet
echo   OK
:jackcess_done
echo.

echo   Installing ucanaccess:4.0.4
if not exist "%CIMUTIL_LIB%\ucanaccess-4.0.4.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\ucanaccess-4.0.4.jar -- skipping
    goto :ucanaccess_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="net.sf.ucanaccess" -DartifactId="ucanaccess" -Dversion="4.0.4" -Dfile="%CIMUTIL_LIB%\ucanaccess-4.0.4.jar" --quiet
echo   OK
:ucanaccess_done
echo.

echo   Installing hsqldb:2.3.6
if not exist "%CIMUTIL_LIB%\hsqldb-2.3.6.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\hsqldb-2.3.6.jar -- skipping
    goto :hsqldb_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="org.hsqldb" -DartifactId="hsqldb" -Dversion="2.3.6" -Dfile="%CIMUTIL_LIB%\hsqldb-2.3.6.jar" --quiet
echo   OK
:hsqldb_done
echo.

echo   Installing gson:2.8.6
if not exist "%CIMUTIL_LIB%\gson-2.8.6.jar" (
    echo   [MISSING] %CIMUTIL_LIB%\gson-2.8.6.jar -- skipping
    goto :gson_done
)
call mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath="%REPO%" -DcreateChecksum=true -Dpackaging=jar -DgroupId="com.google.code.gson" -DartifactId="gson" -Dversion="2.8.6" -Dfile="%CIMUTIL_LIB%\gson-2.8.6.jar" --quiet
echo   OK
:gson_done
echo.

:skip_vendor
:end
echo =======================================================================
echo   Installation complete.
echo   You can now run: mvn clean package
echo =======================================================================

endlocal
