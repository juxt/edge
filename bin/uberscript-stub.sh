#!/bin/sh
JAR_FILE=$(which "$0" 2>/dev/null)
[ $? -gt 0 -a -f "$0" ] && JAR_FILE="./$0"
JAVA_EXE=java
if test -n "$JAVA_HOME"; then
  JAVA_EXE="$JAVA_HOME/bin/java"
fi
$JAVA_EXE $JAVA_ARGS -jar $JAR_FILE -m edge.main $@
exit 0
