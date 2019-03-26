#!/usr/bin/env bash

. `dirname $0`/checkEnv.sh

exec ${JAVA} "-DLSDir="${LSDir} -cp ${LSDir}"/lib/*" LuceneServer "stop"