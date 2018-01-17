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
   KAFKA_HOME="$basepath";
fi

#add lib
for f in $KAFKA_HOME/libs/*.jar
do  
  CLASSPATH=${CLASSPATH}:$f;  
done  

jar_file="$basepath"/obtain-consumer-offsets.jar

if [ ! -f "$jar_file" ]; then
   echo "compile source file........"
   $JAVAC -classpath $CLASSPATH  -sourcepath "$basepath"/sources "$basepath"/sources/*.java -d "$basepath"
   if [ $? == 0 ]; then
      echo "build obtain-consumer-offsets jar.........."
      $JAR -cf  obtain-consumer-offsets.jar  src/consumer/
      rm -rf "$basepath"/src/consumer
      $JAVA -cp ${CLASSPATH}:$jar_file consumer.ObtainConsumerOffsets "$@"
   else
      echo 'compile error....'
      exit 1
   fi
else
   $JAVA -cp ${CLASSPATH}:$jar_file consumer.ObtainConsumerOffsets "$@"
fi
