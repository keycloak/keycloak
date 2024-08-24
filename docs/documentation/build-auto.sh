#!/bin/bash

OPTS=$1

while true; do 
    CHANGED=`inotifywait -r -e modify,move,create,delete authorization_services getting_started server_admin server_development server_installation upgrading --format %w`
    GUIDE=`echo $CHANGED | cut -d '/' -f 1`
    mvn clean install -f $GUIDE $OPTS  
done
