#!/bin/sh
set -e

DEFAULT_JVM_OPTS='"-Xmx2048m" "-Xms512m"'
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"

exec java $DEFAULT_JVM_OPTS -jar gradle/wrapper/gradle-wrapper.jar "$@"
