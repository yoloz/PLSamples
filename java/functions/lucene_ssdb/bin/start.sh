#!/usr/bin/env bash

case $1 in
  -h |-help| --h | --help)
   echo "USAGE: $0 [daemon]"
   exit 1
  ;;
  *)
  ;;
esac

. `dirname $0`/checkEnv.sh

test -d ${LSDir}"/logs" || mkdir -p ${LSDir}"/logs"

if [[ "x$1" = "xdaemon" ]]; then
  shift
  nohup ${JAVA} "-DLSDir="${LSDir} -cp ${LSDir}"/lib/*" HttpServer "start" > ${LSDir}"/logs/server.out" 2>&1 < /dev/null &
else
  exec ${JAVA} "-DLSDir="${LSDir} -cp ${LSDir}"/lib/*" HttpServer "start"
fi