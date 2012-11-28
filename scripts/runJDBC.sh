#!/bin/bash
ORA_JAR=$(find /usr/vertigo/latest/ -name "ojdbc*.jar")

java -cp .:$ORA_JAR com.accertify.socket.MonitorJDBCEndpoint $1 $2 $3

