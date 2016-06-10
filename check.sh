#!/bin/bash

if [ "$1" == "" ]; then
    DOC="target/master.html"
else
    DOC="$1/target/master.html"
fi

for i in `cat $DOC | grep -o -e 'href="[^"]*"' | cut -d '"' -f 2`; do
    if ( echo $i | grep '^#' &>/dev/null ); then
        i=`echo $i | sed 's/#//'`
        if ( ! cat $DOC | grep "id=\"$i\"" &>/dev/null ); then
            echo "Missing link:        $i"
            ERROR=1
        fi
    else
        if ( echo $i | grep 'redhat.com' &>/dev/null ); then
            if ( ! curl --insecure -s $i | grep "attributes.set('Name'" &>/dev/null ); then
                echo "Invalid link:        $i"
                ERROR=1
            fi
        elif ( ! curl --output /dev/null --silent --head --fail "$i" --connect-timeout 2 ); then
            echo "Invalid link:        $i"
            ERROR=1
        fi
    fi
done

if ( cat $DOC | grep ifeval &>/dev/null ); then
    echo "Found ifeval in text"
    ERROR=1
fi

for i in `cat $DOC | grep -o -e '{book_[^}]*}' | sed 's/{//' | sed 's/}//'`; do
    echo "Invalid attribute:   $i"
    ERROR=1
done

if [ $ERROR ]; then
    exit 1
fi
