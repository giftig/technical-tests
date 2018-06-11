#!/bin/bash

mvn clean -DskipTests package || exit $?
java -jar target/noughts-coding-test-0.0.1-SNAPSHOT-bin.jar server dev.yml
