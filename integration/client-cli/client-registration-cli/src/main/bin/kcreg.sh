#!/bin/sh
RESOLVED_NAME="$0"
case "`uname`" in
    CYGWIN*)
        CFILE = `cygpath "$0"`
        RESOLVED_NAME=`readlink -f "$CFILE"`
        ;;
    Darwin*)
        RESOLVED_NAME=`readlink "$0"`
        ;;
    FreeBSD)
        RESOLVED_NAME=`readlink -f "$0"`
        ;;
    Linux)
        RESOLVED_NAME=`readlink -f "$0"`
        ;;
esac

DIRNAME=`dirname "$RESOLVED_NAME"`

java $KC_OPTS -cp $DIRNAME/client/keycloak-client-registration-cli-${project.version}.jar org.keycloak.client.registration.cli.KcRegMain "$@"