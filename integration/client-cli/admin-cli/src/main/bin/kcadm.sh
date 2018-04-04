#!/bin/sh
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

if [ "x$RESOLVED_NAME" = "x" ]; then
    RESOLVED_NAME="$0"
fi

DIRNAME=`dirname "$RESOLVED_NAME"`


# Uncomment out these lines if you are integrating with `kcinit`
#if [ "$1" = "config" ]; then
#    java $KC_OPTS -cp $DIRNAME/client/keycloak-admin-cli-${project.version}.jar org.keycloak.client.admin.cli.KcAdmMain "$@"
#else
#    java $KC_OPTS -cp $DIRNAME/client/keycloak-admin-cli-${project.version}.jar org.keycloak.client.admin.cli.KcAdmMain "$@" --noconfig --token $(kcinit token admin-cli) --server $(kcinit show server)
#fi
# Remove the next line if you have enabled kcinit
java $KC_OPTS -cp $DIRNAME/client/keycloak-admin-cli-${project.version}.jar org.keycloak.client.admin.cli.KcAdmMain "$@"

