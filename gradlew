#!/bin/sh
# Gradle start up script for UN*X

# Attempt to set APP_HOME
APP_HOME=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )/" &> /dev/null && pwd )
APP_NAME="Gradle"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
