#!/usr/bin/env bash
set -e
MAVEN_VERSION=3.9.4
MAVEN_DIR="$(pwd)/.mvn/apache-maven-${MAVEN_VERSION}"
if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  echo "Downloading Maven ${MAVEN_VERSION}..."
  mkdir -p ".mvn"
  TMP=$(mktemp -d)
  ZIP="$TMP/maven.zip"
  curl -fsSL -o "$ZIP" "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.zip"
  unzip -q "$ZIP" -d "$TMP"
  mv "$TMP/apache-maven-${MAVEN_VERSION}" ".mvn/"
  rm -rf "$TMP"
fi
exec "$MAVEN_DIR/bin/mvn" "$@"
