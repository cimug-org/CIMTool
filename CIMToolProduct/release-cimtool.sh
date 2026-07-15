#!/usr/bin/env bash
# =============================================================================
#  release-cimtool.sh
#
#  Packages a Linux (gtk.linux.x86_64) CIMTool Eclipse PDE product export into
#  a distribution archive, and builds + packages the cimtool-cli uber JAR.
#
#  Unlike the Windows release (release-cimtool.bat), the Linux release performs
#  NO code signing: Linux has no authenticode/EV-token equivalent, so artifacts
#  are protected by their SHA-256 checksums alone.
#
#  Steps:
#    1. Populate cimtool-cli lib-repo/ via install-jars.sh
#    2. Build the cimtool-cli uber JAR via Maven
#    3. Deploy cimtool-cli.jar to dist/ and generate its SHA-256 checksum
#    4. Ensure the CIMTool launcher is executable
#    5. Package the CIMToolProduct export into a distribution tar.gz
#    6. Generate the tar.gz SHA-256 checksum
#
#  On completion four artifacts are ready for upload to GitHub Releases:
#    - CIMTool-<version>-linux.gtk.x86_64.tar.gz
#    - CIMTool-<version>-linux.gtk.x86_64.tar.gz.sha256
#    - cimtool-cli.jar
#    - cimtool-cli.jar.sha256
#
#  Prerequisites
#  -------------
#  1. A JDK 20 and Maven 3.9+ on the PATH (to build the cimtool-cli uber JAR).
#  2. A completed Eclipse PDE product export for linux/gtk/x86_64 at
#     <export-root>/<version-string>, produced by the Eclipse Product export
#     wizard with "Export for multiple platforms" -> linux/gtk/x86_64 selected.
#
#  Usage
#  -----
#    ./release-cimtool.sh <export-root> <version-string>
#
#  Example:
#    ./release-cimtool.sh ~/CIMTool-Releases CIMTool-2.3.0
# =============================================================================

set -euo pipefail

# Resolve the directory this script lives in (CIMToolProduct/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# cimtool-cli is a sibling directory of CIMToolProduct under the repo root
CIMTOOL_CLI_DIR="$SCRIPT_DIR/../cimtool-cli"

MVN="${MVN:-mvn}"

echo
echo "============================================================================="
echo " CIMTool Linux Release Packaging"
echo "============================================================================="
echo

# --- Validate parameters ---
EXPORT_ROOT="${1:-}"
VERSION="${2:-}"

usage() {
    echo "Usage:   ./release-cimtool.sh <export-root> <version-string>"
    echo "Example: ./release-cimtool.sh ~/CIMTool-Releases CIMTool-2.3.0"
}

if [ -z "$EXPORT_ROOT" ]; then
    echo "ERROR: Export root directory not specified."
    echo
    usage
    exit 1
fi

if [ -z "$VERSION" ]; then
    echo "ERROR: Version string not specified."
    echo
    usage
    exit 1
fi

PRODUCT_DIR="$EXPORT_ROOT/$VERSION"
PLUGINS_DIR="$PRODUCT_DIR/plugins"
LAUNCHER="$PRODUCT_DIR/CIMTool"

# --- Validate export directory ---
if [ ! -d "$PRODUCT_DIR" ]; then
    echo "ERROR: Product directory not found: $PRODUCT_DIR"
    echo "       Verify the export root and version string are correct."
    exit 1
fi

if [ ! -d "$PLUGINS_DIR" ]; then
    echo "ERROR: Plugins directory not found: $PLUGINS_DIR"
    echo "       Verify that the Eclipse PDE product export completed successfully."
    exit 1
fi

# --- Validate Maven ---
if ! command -v "$MVN" >/dev/null 2>&1; then
    echo "ERROR: Maven ($MVN) not found on PATH."
    echo "       Install Maven 3.9 or later and ensure mvn is on the PATH."
    exit 1
fi

# --- Validate cimtool-cli directory ---
if [ ! -f "$CIMTOOL_CLI_DIR/pom.xml" ]; then
    echo "ERROR: cimtool-cli project not found at $CIMTOOL_CLI_DIR"
    echo "       Ensure this script is run from within the CIMToolProduct directory"
    echo "       and that cimtool-cli is a sibling directory under the repo root."
    exit 1
fi

# --- Step 1: Populate cimtool-cli lib-repo ---
echo "[Step 1/6] Populating cimtool-cli lib-repo via install-jars.sh..."
echo "           Export root: $PRODUCT_DIR"
echo
bash "$CIMTOOL_CLI_DIR/install-jars.sh" "$PRODUCT_DIR"

# --- Step 2: Build cimtool-cli uber JAR ---
echo
echo "[Step 2/6] Building cimtool-cli uber JAR..."
( cd "$CIMTOOL_CLI_DIR" && "$MVN" clean package -q )

CLI_JAR="$CIMTOOL_CLI_DIR/target/cimtool-cli.jar"
if [ ! -f "$CLI_JAR" ]; then
    echo "ERROR: Expected uber JAR not found at $CLI_JAR"
    exit 1
fi
echo "  OK: $CLI_JAR"

# --- Step 3: Deploy cimtool-cli.jar to dist/ and generate SHA-256 ---
CLI_DIST_DIR="$CIMTOOL_CLI_DIR/dist"
CLI_DIST_JAR="$CLI_DIST_DIR/cimtool-cli.jar"
CLI_SHA256="$CLI_DIST_DIR/cimtool-cli.jar.sha256"

echo
echo "[Step 3/6] Deploying to dist/ and generating SHA-256 checksum..."
echo "           $CLI_DIST_JAR"

mkdir -p "$CLI_DIST_DIR"
cp -f "$CLI_JAR" "$CLI_DIST_JAR"

( cd "$CLI_DIST_DIR" && sha256sum "cimtool-cli.jar" > "$CLI_SHA256" )
echo "           $CLI_SHA256"

# --- Step 4: Ensure the launcher is executable ---
echo
echo "[Step 4/6] Ensuring CIMTool launcher is executable..."
if [ ! -f "$LAUNCHER" ]; then
    echo "ERROR: CIMTool launcher not found at $LAUNCHER"
    exit 1
fi
chmod +x "$LAUNCHER"

# --- Step 5: Package tar.gz ---
ARCHIVE_NAME="$VERSION-linux.gtk.x86_64.tar.gz"
ARCHIVE_PATH="$EXPORT_ROOT/$ARCHIVE_NAME"

echo
echo "[Step 5/6] Creating distribution archive..."
echo "           $ARCHIVE_PATH"

# Archive the versioned product directory relative to the export root so the
# tarball unpacks to a single top-level <version> directory. tar preserves the
# executable bit on the launcher, unlike a plain zip.
tar -C "$EXPORT_ROOT" -czf "$ARCHIVE_PATH" "$VERSION"

# --- Step 6: Generate tar.gz SHA-256 checksum ---
SHA256_PATH="$ARCHIVE_PATH.sha256"

echo
echo "[Step 6/6] Generating archive SHA-256 checksum..."
echo "           $SHA256_PATH"

( cd "$EXPORT_ROOT" && sha256sum "$ARCHIVE_NAME" > "$SHA256_PATH" )

# --- Summary ---
echo
echo "============================================================================="
echo " Packaging complete."
echo
echo " CIMToolProduct artifacts:"
echo "   Archive:   $ARCHIVE_PATH"
echo "   Checksum:  $SHA256_PATH"
echo
echo " cimtool-cli artifacts:"
echo "   JAR:       $CLI_DIST_JAR"
echo "   Checksum:  $CLI_SHA256"
echo
echo " Upload all four files to the GitHub release at:"
echo " https://github.com/cimug-org/CIMTool/releases"
echo "============================================================================="
echo
