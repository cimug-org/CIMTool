#!/usr/bin/env bash
# =============================================================================
# install-jars.sh
#
# Installs kena.jar and cimutil.jar from a CIMTool Eclipse PDE product export,
# plus all vendored JARs from Kena/lib/ and CIMUtil/lib/, into the local Maven
# file repository at cimtool-cli/lib-repo/.
#
# kena.jar and cimutil.jar are located by searching the plugins/ directory of
# the PDE export for plugin folders matching au.com.langdale.kena_* and
# au.com.langdale.cimutil_*. The version number is extracted from the plugin
# folder name (e.g. au.com.langdale.kena_3.3.4 -> 3.3.4) rather than from
# the JAR filename, since the JARs are named kena.jar and cimutil.jar without
# a version suffix.
#
# NOTE: The version extracted from the plugin folder name must match the
# <version> declared for kena and cimutil in pom.xml. Update pom.xml if
# the version changes.
#
# Usage:
#   ./install-jars.sh <export-root>
#
#   <export-root>  Path to the root of the Eclipse PDE product export.
#                  If omitted, the script will prompt for it interactively.
#
# Examples:
#   ./install-jars.sh /opt/CIMTool-Releases/CIMTool-2.3.0
#   ./install-jars.sh "/opt/My Releases/CIMTool-2.3.0"
#
# Prerequisites:
#   - mvn must be on your PATH
#   - A CIMTool Eclipse PDE product export must exist at <export-root>
#     with a plugins/ subdirectory containing au.com.langdale.kena_* and
#     au.com.langdale.cimutil_* plugin folders
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$SCRIPT_DIR/lib-repo"
KENA_LIB="$SCRIPT_DIR/../Kena/lib"
CIMUTIL_LIB="$SCRIPT_DIR/../CIMUtil/lib"
CIMTOOLE_LIB="$SCRIPT_DIR/../CIMToolPlugin/lib"

# -----------------------------------------------------------------------
# Resolve the export root — from argument or interactive prompt
# -----------------------------------------------------------------------
if [ -z "$1" ]; then
    echo
    echo "  No export root specified."
    echo "  Enter the path to the CIMTool Eclipse PDE product export root."
    echo "  Example: /opt/CIMTool-Releases/CIMTool-2.3.0"
    echo
    read -rp "  Export root: " EXPORT_ROOT
else
    EXPORT_ROOT="$1"
fi

echo
echo "======================================================================="
echo "  CIMTool CLI -- Local Repository Installer"
echo "  Repository:   $REPO"
echo "  Export root:  $EXPORT_ROOT"
echo "======================================================================="
echo

# Validate the export root exists
if [ ! -d "$EXPORT_ROOT" ]; then
    echo "[ERROR] Export root not found: $EXPORT_ROOT"
    echo "        Check the path and try again."
    exit 1
fi

PLUGINS_DIR="$EXPORT_ROOT/plugins"
if [ ! -d "$PLUGINS_DIR" ]; then
    echo "[ERROR] plugins/ directory not found under: $EXPORT_ROOT"
    echo "        Ensure this is the root of a CIMTool PDE product export."
    exit 1
fi

MVN_INSTALL="mvn install:install-file -Daether.checksums.algorithms=SHA-256,SHA-1,MD5 -DlocalRepositoryPath=$REPO -DcreateChecksum=true -Dpackaging=jar"

# -----------------------------------------------------------------------
# Purge stale kena and cimutil entries from lib-repo/ and ~/.m2
# -----------------------------------------------------------------------
[ -d "$REPO/au/com/langdale/kena" ]   && rm -rf "$REPO/au/com/langdale/kena"
[ -d "$REPO/au/com/langdale/cimutil" ] && rm -rf "$REPO/au/com/langdale/cimutil"
[ -d "$HOME/.m2/repository/au/com/langdale/kena" ]   && rm -rf "$HOME/.m2/repository/au/com/langdale/kena"
[ -d "$HOME/.m2/repository/au/com/langdale/cimutil" ] && rm -rf "$HOME/.m2/repository/au/com/langdale/cimutil"

# -----------------------------------------------------------------------
# CIMTool core modules — sourced from the PDE product export plugins/
# -----------------------------------------------------------------------
echo "--- CIMTool core modules -------------------------------------------"
echo

echo "  Installing kena"
KENA_JAR=""
KENA_VERSION=""
for d in "$PLUGINS_DIR"/au.com.langdale.kena_*/; do
    if [ -f "${d}kena.jar" ]; then
        KENA_JAR="${d}kena.jar"
        KENA_FOLDER=$(basename "$d")
        KENA_VERSION="${KENA_FOLDER#au.com.langdale.kena_}"
    fi
done
if [ -z "$KENA_JAR" ]; then
    echo "  [MISSING] No au.com.langdale.kena_* folder containing kena.jar found under $PLUGINS_DIR"
    echo "            Ensure the PDE export completed successfully."
else
    echo "  Found: $KENA_JAR"
    echo "  Version: $KENA_VERSION"
    $MVN_INSTALL -DgroupId="au.com.langdale" -DartifactId="kena" -Dversion="$KENA_VERSION" -Dfile="$KENA_JAR" --quiet
    echo "  OK"
fi
echo

echo "  Installing cimutil"
CIMUTIL_JAR=""
CIMUTIL_VERSION=""
for d in "$PLUGINS_DIR"/au.com.langdale.cimutil_*/; do
    if [ -f "${d}cimutil.jar" ]; then
        CIMUTIL_JAR="${d}cimutil.jar"
        CIMUTIL_FOLDER=$(basename "$d")
        CIMUTIL_VERSION="${CIMUTIL_FOLDER#au.com.langdale.cimutil_}"
    fi
done
if [ -z "$CIMUTIL_JAR" ]; then
    echo "  [MISSING] No au.com.langdale.cimutil_* folder containing cimutil.jar found under $PLUGINS_DIR"
    echo "            Ensure the PDE export completed successfully."
else
    echo "  Found: $CIMUTIL_JAR"
    echo "  Version: $CIMUTIL_VERSION"
    $MVN_INSTALL -DgroupId="au.com.langdale" -DartifactId="cimutil" -Dversion="$CIMUTIL_VERSION" -Dfile="$CIMUTIL_JAR" --quiet
    echo "  OK"
fi
echo

# -----------------------------------------------------------------------
# Prompt user before installing vendor JARs
# -----------------------------------------------------------------------
echo
echo "  The vendor JARs (Kena/lib and CIMUtil/lib) rarely change between"
echo "  releases. Re-installing them is only needed if new or updated JARs"
echo "  have been added to those directories since the last run."
echo "  If unsure, answer Y to ensure lib-repo is fully up to date."
echo
read -rp "  Install vendor JARs? (Y/N): " INSTALL_VENDOR
if [[ ! "$INSTALL_VENDOR" =~ ^[Yy]$ ]]; then
    echo "======================================================================="
    echo "  Installation complete."
    echo "  You can now run: mvn clean package"
    echo "======================================================================="
    exit 0
fi

# -----------------------------------------------------------------------
# Kena/lib
# Note: jena-2.6.3-tests.jar, iri-0.8-sources.jar, and junit-4.5.jar
#       are intentionally omitted. See pom.xml.
#       log4j-1.2.13.jar and slf4j-log4j12-1.5.8.jar have been replaced
#       by log4j-over-slf4j-2.0.17.jar. See pom.xml.
# -----------------------------------------------------------------------
echo "--- Kena/lib -------------------------------------------------------"
echo

install_jar() {
    local label="$1" file="$2" groupId="$3" artifactId="$4" version="$5"
    echo "  Installing $label"
    if [ ! -f "$file" ]; then
        echo "  [MISSING] $file -- skipping"
    else
        $MVN_INSTALL -DgroupId="$groupId" -DartifactId="$artifactId" -Dversion="$version" -Dfile="$file" --quiet
        echo "  OK"
    fi
    echo
}

install_jar "jena:2.6.3"         "$KENA_LIB/jena-2.6.3.jar"         "com.hp.hpl.jena"    "jena"           "2.6.3"
install_jar "arq:2.8.4"          "$KENA_LIB/arq-2.8.4.jar"          "com.hp.hpl.jena"    "arq"            "2.8.4"
install_jar "iri:0.8"            "$KENA_LIB/iri-0.8.jar"            "com.hp.hpl.jena"    "iri"            "0.8"
install_jar "lucene-core:2.3.1"  "$KENA_LIB/lucene-core-2.3.1.jar"  "org.apache.lucene"  "lucene-core"    "2.3.1"
install_jar "stax-api:1.0.1"     "$KENA_LIB/stax-api-1.0.1.jar"     "javax.xml.stream"   "stax-api"       "1.0.1"
install_jar "wstx-asl:3.2.9"     "$KENA_LIB/wstx-asl-3.2.9.jar"     "org.codehaus.woodstox" "wstx-asl"   "3.2.9"
install_jar "xercesImpl:2.7.1"   "$KENA_LIB/xercesImpl-2.7.1.jar"   "xerces"             "xercesImpl"     "2.7.1"
install_jar "icu4j:71.1"         "$KENA_LIB/icu4j-71.1.jar"         "com.ibm.icu"        "icu4j"          "71.1"
install_jar "log4j-over-slf4j:2.0.17" "$KENA_LIB/log4j-over-slf4j-2.0.17.jar" "org.slf4j" "log4j-over-slf4j" "2.0.17"

# -----------------------------------------------------------------------
# SLF4J 2.x / Logback / JUL bridge
# Sourced from the PDE product export plugins/ directory (wildcard match
# on bundle version qualifier) and from CIMToolPlugin/lib/.
# -----------------------------------------------------------------------
echo "--- SLF4J 2.x / Logback / JUL bridge ------------------------------"
echo

echo "  Installing slf4j-api:2.0.17"
SLF4J_API_JAR=$(find "$PLUGINS_DIR" -maxdepth 1 -name "org.slf4j.api_2.0.17*.jar" 2>/dev/null | head -1)
if [ -z "$SLF4J_API_JAR" ]; then
    echo "  [MISSING] No org.slf4j.api_2.0.17*.jar found under $PLUGINS_DIR -- skipping"
else
    echo "  Found: $SLF4J_API_JAR"
    $MVN_INSTALL -DgroupId="org.slf4j" -DartifactId="slf4j-api" -Dversion="2.0.17" -Dfile="$SLF4J_API_JAR" --quiet
    echo "  OK"
fi
echo

echo "  Installing logback-core:1.5.32"
LOGBACK_CORE_JAR=$(find "$PLUGINS_DIR" -maxdepth 1 -name "ch.qos.logback.core_1.5.32*.jar" 2>/dev/null | head -1)
if [ -z "$LOGBACK_CORE_JAR" ]; then
    echo "  [MISSING] No ch.qos.logback.core_1.5.32*.jar found under $PLUGINS_DIR -- skipping"
else
    echo "  Found: $LOGBACK_CORE_JAR"
    $MVN_INSTALL -DgroupId="ch.qos.logback" -DartifactId="logback-core" -Dversion="1.5.32" -Dfile="$LOGBACK_CORE_JAR" --quiet
    echo "  OK"
fi
echo

echo "  Installing logback-classic:1.5.32"
LOGBACK_CLASSIC_JAR=$(find "$PLUGINS_DIR" -maxdepth 1 -name "ch.qos.logback.classic_1.5.32*.jar" 2>/dev/null | head -1)
if [ -z "$LOGBACK_CLASSIC_JAR" ]; then
    echo "  [MISSING] No ch.qos.logback.classic_1.5.32*.jar found under $PLUGINS_DIR -- skipping"
else
    echo "  Found: $LOGBACK_CLASSIC_JAR"
    $MVN_INSTALL -DgroupId="ch.qos.logback" -DartifactId="logback-classic" -Dversion="1.5.32" -Dfile="$LOGBACK_CLASSIC_JAR" --quiet
    echo "  OK"
fi
echo

install_jar "jul-to-slf4j:2.0.17" "$CIMTOOLE_LIB/jul-to-slf4j-2.0.17.jar" "org.slf4j" "jul-to-slf4j" "2.0.17"

# -----------------------------------------------------------------------
# CIMUtil/lib
# -----------------------------------------------------------------------
echo "--- CIMUtil/lib ----------------------------------------------------"
echo

install_jar "Saxon-HE:10.8"           "$CIMUTIL_LIB/saxon-he-10.8.jar"          "net.sf.saxon"          "Saxon-HE"       "10.8"
install_jar "easy-rules-core:4.1.0"   "$CIMUTIL_LIB/easy-rules-core-4.1.0.jar"  "org.jeasy"             "easy-rules-core"    "4.1.0"
install_jar "easy-rules-support:4.1.0" "$CIMUTIL_LIB/easy-rules-support-4.1.0.jar" "org.jeasy"          "easy-rules-support" "4.1.0"
install_jar "poi:3.9"                 "$CIMUTIL_LIB/poi-3.9.jar"                "org.apache.poi"        "poi"            "3.9"
install_jar "commons-lang:2.6"        "$CIMUTIL_LIB/commons-lang-2.6.jar"       "commons-lang"          "commons-lang"   "2.6"
install_jar "commons-logging:1.1.3"   "$CIMUTIL_LIB/commons-logging-1.1.3.jar"  "commons-logging"       "commons-logging" "1.1.3"
install_jar "xml-resolver:1.2"        "$CIMUTIL_LIB/xml-resolver-1.2.jar"       "xml-resolver"          "xml-resolver"   "1.2"
install_jar "sqlite-jdbc:3.45.2.0"    "$CIMUTIL_LIB/sqlite-jdbc-3.45.2.0.jar"   "org.xerial"            "sqlite-jdbc"    "3.45.2.0"
install_jar "jackcess:2.2.3"          "$CIMUTIL_LIB/jackcess-2.2.3.jar"         "com.healthmarketscience.jackcess" "jackcess" "2.2.3"
install_jar "ucanaccess:4.0.4"        "$CIMUTIL_LIB/ucanaccess-4.0.4.jar"       "net.sf.ucanaccess"     "ucanaccess"     "4.0.4"
install_jar "hsqldb:2.3.6"            "$CIMUTIL_LIB/hsqldb-2.3.6.jar"           "org.hsqldb"            "hsqldb"         "2.3.6"
install_jar "gson:2.8.6"              "$CIMUTIL_LIB/gson-2.8.6.jar"             "com.google.code.gson"  "gson"           "2.8.6"

echo "======================================================================="
echo "  Installation complete."
echo "  You can now run: mvn clean package"
echo "======================================================================="
