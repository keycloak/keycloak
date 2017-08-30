#!/bin/bash

DIR=`dirname $0`/src/web

while true; do 
    CHANGED=`inotifywait -r -e modify,move,create,delete server_installation --format %w`
    GUIDE=`dirname $CHANGED`
    mvn install -f $GUIDE    
done
