#! /bin/sh
WS=/home/projects/CIMTool
JARS=$(echo $WS/CIMToolPlugin/lib/*.jar | sed -e 's/ /:/g')
CP=$WS/CIMToolPlugin/bin/:$WS/CIMToolTest/bin/:$JARS
java -cp $CP $*
