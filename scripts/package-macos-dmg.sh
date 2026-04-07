#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

APP_NAME="DMHelper"
APP_VERSION="1.0.0"
MAIN_CLASS="Main"
SQLITE_VERSION="3.51.3.0"
JAVAFX_VERSION="22.0.2"

ARCH="$(uname -m)"
if [[ "$ARCH" == "arm64" ]]; then
  JAVAFX_CLASSIFIER="mac-aarch64"
elif [[ "$ARCH" == "x86_64" ]]; then
  JAVAFX_CLASSIFIER="mac"
else
  echo "Unsupported macOS architecture: $ARCH" >&2
  exit 1
fi

M2_REPO="${HOME}/.m2/repository"
JAVAFX_BASE_JAR="${M2_REPO}/org/openjfx/javafx-base/${JAVAFX_VERSION}/javafx-base-${JAVAFX_VERSION}-${JAVAFX_CLASSIFIER}.jar"
JAVAFX_CONTROLS_JAR="${M2_REPO}/org/openjfx/javafx-controls/${JAVAFX_VERSION}/javafx-controls-${JAVAFX_VERSION}-${JAVAFX_CLASSIFIER}.jar"
JAVAFX_GRAPHICS_JAR="${M2_REPO}/org/openjfx/javafx-graphics/${JAVAFX_VERSION}/javafx-graphics-${JAVAFX_VERSION}-${JAVAFX_CLASSIFIER}.jar"
SQLITE_JAR="${M2_REPO}/org/xerial/sqlite-jdbc/${SQLITE_VERSION}/sqlite-jdbc-${SQLITE_VERSION}.jar"

for required in \
  "$JAVAFX_BASE_JAR" \
  "$JAVAFX_CONTROLS_JAR" \
  "$JAVAFX_GRAPHICS_JAR" \
  "$SQLITE_JAR"
do
  if [[ ! -f "$required" ]]; then
    echo "Missing dependency jar: $required" >&2
    echo "Please download dependencies first, for example by running Maven once in this project." >&2
    exit 1
  fi
done

BUILD_DIR="${ROOT_DIR}/target/jpackage"
CLASSES_DIR="${ROOT_DIR}/target/classes"
INPUT_DIR="${BUILD_DIR}/input"
DIST_DIR="${BUILD_DIR}/dist"
TMP_CLASSES_DIR="${BUILD_DIR}/classes"
ICON_PATH="${CLASSES_DIR}/com/DMHelper/assets/app_icon.icns"

rm -rf "$BUILD_DIR"
mkdir -p "$INPUT_DIR" "$DIST_DIR" "$TMP_CLASSES_DIR"

echo "Compiling application classes..."
javac --release 17 \
  -cp "${JAVAFX_BASE_JAR}:${JAVAFX_CONTROLS_JAR}:${JAVAFX_GRAPHICS_JAR}:${SQLITE_JAR}" \
  -d "$TMP_CLASSES_DIR" \
  $(find src/main/java -name '*.java')

echo "Copying packaged resources..."
mkdir -p "${TMP_CLASSES_DIR}/com/DMHelper/basic/javafx" "${TMP_CLASSES_DIR}/com/DMHelper/assets"
cp src/main/java/com/DMHelper/basic/javafx/main-menu.css "${TMP_CLASSES_DIR}/com/DMHelper/basic/javafx/"
cp src/main/java/com/DMHelper/assets/app_icon.png "${TMP_CLASSES_DIR}/com/DMHelper/assets/"
cp src/main/java/com/DMHelper/assets/app_icon.ico "${TMP_CLASSES_DIR}/com/DMHelper/assets/"
cp src/main/java/com/DMHelper/assets/app_icon.icns "${TMP_CLASSES_DIR}/com/DMHelper/assets/"

echo "Creating application jar..."
jar --create --file "${INPUT_DIR}/dmhelper-app.jar" -C "$TMP_CLASSES_DIR" .
cp "$SQLITE_JAR" "$INPUT_DIR/"

echo "Building macOS app image..."
jpackage \
  --type app-image \
  --dest "$DIST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "$APP_NAME" \
  --main-jar dmhelper-app.jar \
  --main-class "$MAIN_CLASS" \
  --icon "$ICON_PATH" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "--add-modules=javafx.controls,javafx.graphics" \
  --module-path "${JAVAFX_BASE_JAR}:${JAVAFX_CONTROLS_JAR}:${JAVAFX_GRAPHICS_JAR}" \
  --add-modules javafx.controls,javafx.graphics

echo "Building DMG installer..."
jpackage \
  --type dmg \
  --dest "$DIST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "$APP_NAME" \
  --main-jar dmhelper-app.jar \
  --main-class "$MAIN_CLASS" \
  --icon "$ICON_PATH" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "--add-modules=javafx.controls,javafx.graphics" \
  --module-path "${JAVAFX_BASE_JAR}:${JAVAFX_CONTROLS_JAR}:${JAVAFX_GRAPHICS_JAR}" \
  --add-modules javafx.controls,javafx.graphics

echo
echo "Done."
echo "App image: ${DIST_DIR}/${APP_NAME}.app"
echo "DMG file:  ${DIST_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
