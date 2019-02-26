#!/usr/bin/env bash
if [[ $# -lt 1 ]]; then
    echo "USAGE: $0 ssdbUtils.properties"
    exit 1
fi
dir=$(cd `dirname $0`;pwd)
#如果环境中未配置JAVA_HOME,可在下面单引号里添加jdk的路径
#export JAVA_HOME=$dir'/jdk1.8.0_151'
if [[ "x$JAVA_HOME" = "x" ]]; then
    echo "JAVA_HOME is not configured, please configure and then execute again!"
    exit 1
else
    export JAVA=${JAVA_HOME}/bin/java
fi
java_version=`${JAVA} -version 2>&1 |awk 'NR==1{gsub(/"/,""); print $3}'`
java_version=${java_version:0:3}
if [[ ${java_version//[_|.]/} -lt 18 ]]; then
    echo "java version need 1.8+"
    exit 1;
fi
if [[ ! -x ${JAVA:=''} ]]; then
    echo "java command error..."
    exit 1
fi
case $1 in
    -h | --h | --help)
        echo "USAGE: $0 ssdbUtils.properties"
        exit 1
    ;;
    *)
    ;;
esac
if [[ ! -f $1 ]]; then
    echo "$1 does not exit or empty..."
    echo "USAGE: $0 ssdbUtils.properties"
    exit 1
fi

for f in `ls $dir/lib`
do
    CLASSPATH=${CLASSPATH}:$dir"/lib/"${f}
done

log_dir=${dir}/logs
#echo "Log recorded in $log_dir"
test -d ${log_dir} || mkdir -p ${log_dir}
APP_DIR="-DjarPath=$dir"
nohup ${JAVA} ${APP_DIR} -cp ${CLASSPATH}  SSDBUtils "$@" > "$dir/nohup" 2>&1 < /dev/null &
