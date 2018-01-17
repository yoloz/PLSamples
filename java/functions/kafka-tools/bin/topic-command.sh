#!/usr/bin/env bash
shopt -s nullglob
basepath=$(cd `dirname $0`; pwd)

echo "determine env.........."

if [ -z "$JAVA_HOME" ]; then
   echo "Your JAVA_HOME is not configured, please configure and then execute again!"
   exit 1
else
   JAVAC="$JAVA_HOME/bin/javac"
   JAVA="$JAVA_HOME/bin/java"
   JAR="$JAVA_HOME/bin/jar"
fi

if [ -z "$KAFKA_HOME" ]; then
   KAFKA_HOME="$basepath"/libs/;
fi

#add lib
for f in $KAFKA_HOME/libs/*.jar
do  
   CLASSPATH=${CLASSPATH}:$f;  
done  

jar_file="$basepath"/topic-tool.jar

if [ ! -f "$jar_file" ]; then
   echo "Compile source file........"
   $JAVAC -classpath $CLASSPATH  -sourcepath "$basepath"/sources "$basepath"/sources/*.java -d "$basepath"

   echo "Create KafkaUtils jar.........."
   $JAR -cf  topic-tool.jar  src/topic
   rm -rf "$basepath"/src/topic
fi

CLASSPATH=${CLASSPATH}:$jar_file

$JAVA -cp $CLASSPATH com.ethan.kafka.topic_command.Command "$@"
