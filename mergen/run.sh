#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR
mvn exec:java -Dexec.mainClass=com.github.mergen.server.Server -Dexec.args="-hz 127.0.0.1:5701 -hz 127.0.0.1:5702"
