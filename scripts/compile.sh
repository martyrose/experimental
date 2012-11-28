#!/bin/bash

rm -f $(find . -name "*.class")
JAVA_FILES=$(find com -name "Monitor*.java")

javac -cp . $JAVA_FILES
