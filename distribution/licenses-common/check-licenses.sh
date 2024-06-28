#!/bin/bash

MODULES=`readlink -f $1`
LICENSES=`readlink -f $2`

for i in `find $MODULES -name '*.jar'`; do
	if ( unzip -l $i | grep pom.properties &>/dev/null ); then
		unzip -p $i '*/pom.properties' > /tmp/pom.properties
		source /tmp/pom.properties &>/dev/null

		if [ ! -f $LICENSES/$groupId\,$artifactId\,$version,* ]; then	
			echo "Missing: "
			echo "group: $groupId"
			echo "artifact: $artifactId"
			echo "version: $version"
			echo "---------------------"
		fi
	else
		echo "Not Maven: $i"
		echo "---------------------"
	fi


done
