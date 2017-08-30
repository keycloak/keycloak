#!/bin/bash

while true; do 
    CHANGED=`inotifywait -r -e modify,move,create,delete authorization_services getting_started securing_apps server_admin server_development server_installation --format %w`
    GUIDE=`echo $CHANGED | cut -d '/' -f 1`
    mvn install -f $GUIDE    
done
