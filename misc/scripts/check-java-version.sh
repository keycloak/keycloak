#!/bin/bash -e

ZIP=$1
JAVAV=$2

if [ "$ZIP" == "" ]; then
	echo "check-java-version.sh <adapter zip> <java version>"
	exit 1
fi

if [ "$JAVAV" == "" ]; then
	echo "check-java-version.sh <adapter zip> <java version>"
	exit 1
fi

ZIP=`readlink -f $ZIP`

TMP=`mktemp -d`

cd $TMP

unzip -q $ZIP

INVALID_ZIP=0

for i in `find -name '*.jar'`; do
	mkdir t
	unzip -q $i -d t
	
	INVALID=0
	
	for j in `find t/ -name '*.class'`; do
		V=`file "$j" | sed 's/.*version //' | sed 's/.0 (.*//'`
		if [ "$V" -gt $JAVAV ]; then
			INVALID=1
			INVALID_ZIP=1
			INVALID_VERSION=$V
		fi
	done	

	if [ "$INVALID" == "1" ]; then
		echo "[ERROR] $i ($INVALID_VERSION)"
	fi

	rm -rf t
done

if [ "$INVALID_ZIP" == "1" ]; then
	echo ""
	echo "ZIP contains invalid JARs"
	exit 1
fi

cd /tmp
rm -rf $TMP
