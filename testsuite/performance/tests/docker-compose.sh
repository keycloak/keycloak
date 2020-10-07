#!/bin/bash

### FUNCTIONS ###

function runCommand() {
    echo "$1"
    if ! eval "$1" ; then
        echo "Execution of command failed."
        echo "Command: \"$1\""
        exit 1;
    fi
}

function generateDockerComposeFile() {
    echo "Generating $( basename "$DOCKER_COMPOSE_FILE" )"
    local TEMPLATES_PATH=$DEPLOYMENT
    cat $TEMPLATES_PATH/docker-compose-base.yml > $DOCKER_COMPOSE_FILE
    case "$DEPLOYMENT" in
        cluster)
            I=0
            for CPUSET in $KEYCLOAK_CPUSETS ; do
                I=$((I+1))
                sed -e s/%I%/$I/ -e s/%CPUSET%/$CPUSET/ $TEMPLATES_PATH/docker-compose-keycloak.yml >> $DOCKER_COMPOSE_FILE
            done
        ;;
        crossdc) 
            I=0
            for CPUSET in $KEYCLOAK_DC1_CPUSETS ; do
                I=$((I+1))
                sed -e s/%I%/$I/ -e s/%CPUSET%/$CPUSET/ $TEMPLATES_PATH/docker-compose-keycloak_dc1.yml >> $DOCKER_COMPOSE_FILE
            done
            I=0
            for CPUSET in $KEYCLOAK_DC2_CPUSETS ; do
                I=$((I+1))
                sed -e s/%I%/$I/ -e s/%CPUSET%/$CPUSET/ $TEMPLATES_PATH/docker-compose-keycloak_dc2.yml >> $DOCKER_COMPOSE_FILE
            done
        ;;
    esac
}

function inspectDockerPortMapping() {
    local PORT="$1"
    local CONTAINER="$2"
    local INSPECT_COMMAND="docker inspect --format='{{(index (index .NetworkSettings.Ports \"$PORT\") 0).HostPort}}' $CONTAINER"
    MAPPED_PORT="$( eval "$INSPECT_COMMAND" )"
    if [ -z "$MAPPED_PORT" ]; then 
        echo "Error finding mapped port for $CONTAINER."
        exit 1
    fi
}

function generateProvisionedSystemProperties() {
    echo "Generating $PROVISIONED_SYSTEM_PROPERTIES_FILE"
    echo "deployment=$DEPLOYMENT" > $PROVISIONED_SYSTEM_PROPERTIES_FILE
    echo "# Docker Compose" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
    echo "keycloak.docker.services=$KEYCLOAK_SERVICES" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
    case "$DEPLOYMENT" in
        singlenode)
            echo "# HTTP" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            inspectDockerPortMapping 8080/tcp ${PROJECT_NAME}_keycloak_1
            echo "keycloak.frontend.servers=http://localhost:$MAPPED_PORT/auth" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE

            echo "# JMX" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_keycloak_1
            echo "keycloak.frontend.servers.jmx=service:jmx:remote+http://localhost:$MAPPED_PORT" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
        ;;
        cluster)
            echo "# HTTP" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            inspectDockerPortMapping 8080/tcp ${PROJECT_NAME}_loadbalancer_1
            echo "keycloak.frontend.servers=http://localhost:$MAPPED_PORT/auth" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            BACKEND_URLS=""
            for SERVICE in $KEYCLOAK_SERVICES ; do
                inspectDockerPortMapping 8080/tcp ${PROJECT_NAME}_${SERVICE}_1
                BACKEND_URLS="$BACKEND_URLS http://localhost:$MAPPED_PORT/auth"
            done
            echo "keycloak.backend.servers=$BACKEND_URLS" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE

            echo "# JMX" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_loadbalancer_1
            echo "keycloak.frontend.servers.jmx=service:jmx:remote+http://localhost:$MAPPED_PORT" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            BACKEND_URLS=""
            for SERVICE in $KEYCLOAK_SERVICES ; do
                inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_${SERVICE}_1
                BACKEND_URLS="$BACKEND_URLS service:jmx:remote+http://localhost:$MAPPED_PORT"
            done
            echo "keycloak.backend.servers.jmx=$BACKEND_URLS" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
        ;;
        crossdc) 
            echo "# HTTP" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            inspectDockerPortMapping 8080/tcp ${PROJECT_NAME}_loadbalancer_dc1_1
            KC_DC1_PORT=$MAPPED_PORT
            inspectDockerPortMapping 8080/tcp ${PROJECT_NAME}_loadbalancer_dc2_1
            KC_DC2_PORT=$MAPPED_PORT
            echo "keycloak.frontend.servers=http://localhost:$KC_DC1_PORT/auth http://localhost:$KC_DC2_PORT/auth" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            BACKEND_URLS=""
            for SERVICE in $KEYCLOAK_SERVICES ; do
                inspectDockerPortMapping 8080/tcp ${PROJECT_NAME}_${SERVICE}_1
                BACKEND_URLS="$BACKEND_URLS http://localhost:$MAPPED_PORT/auth"
            done
            echo "keycloak.backend.servers=$BACKEND_URLS" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE

            echo "# JMX" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_loadbalancer_dc1_1
            KC_DC1_PORT=$MAPPED_PORT
            inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_loadbalancer_dc2_1
            KC_DC2_PORT=$MAPPED_PORT
            echo "keycloak.frontend.servers.jmx=service:jmx:remote+http://localhost:$KC_DC1_PORT service:jmx:remote+http://localhost:$KC_DC2_PORT" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
            BACKEND_URLS=""
            for SERVICE in $KEYCLOAK_SERVICES ; do
                inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_${SERVICE}_1
                BACKEND_URLS="$BACKEND_URLS service:jmx:remote+http://localhost:$MAPPED_PORT"
            done
            echo "keycloak.backend.servers.jmx=$BACKEND_URLS" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE

            inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_infinispan_dc1_1
            ISPN_DC1_PORT=$MAPPED_PORT
            inspectDockerPortMapping 9990/tcp ${PROJECT_NAME}_infinispan_dc2_1
            ISPN_DC2_PORT=$MAPPED_PORT
            echo "infinispan.servers.jmx=service:jmx:remote+http://localhost:$ISPN_DC1_PORT service:jmx:remote+http://localhost:$ISPN_DC2_PORT" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
        ;;
    esac
    echo "keycloak.admin.user=$KEYCLOAK_ADMIN_USER" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
    echo "keycloak.admin.password=$KEYCLOAK_ADMIN_PASSWORD" >> $PROVISIONED_SYSTEM_PROPERTIES_FILE
}

function loadProvisionedSystemProperties() {
    if [ -f $PROVISIONED_SYSTEM_PROPERTIES_FILE ]; then 
        echo "Loading $PROVISIONED_SYSTEM_PROPERTIES_FILE"
        export DEPLOYMENT=$( sed -n -e '/deployment=/ s/.*\= *//p' $PROVISIONED_SYSTEM_PROPERTIES_FILE )
        export KEYCLOAK_SERVICES=$( sed -n -e '/keycloak.docker.services=/ s/.*\= *//p' $PROVISIONED_SYSTEM_PROPERTIES_FILE )
        export KEYCLOAK_ADMIN_USER=$( sed -n -e '/keycloak.admin.user=/ s/.*\= *//p' $PROVISIONED_SYSTEM_PROPERTIES_FILE )
        export KEYCLOAK_ADMIN_PASSWORD=$( sed -n -e '/keycloak.admin.password=/ s/.*\= *//p' $PROVISIONED_SYSTEM_PROPERTIES_FILE )
    else
        echo "$PROVISIONED_SYSTEM_PROPERTIES_FILE not found."
    fi
}

function removeProvisionedSystemProperties() {
    rm -f $PROVISIONED_SYSTEM_PROPERTIES_FILE
}

function isTestDeployment() {
    IS_TEST_DEPLOYMENT=false
    case "$DEPLOYMENT" in
        singlenode|cluster|crossdc) IS_TEST_DEPLOYMENT=true ;;
    esac
    $IS_TEST_DEPLOYMENT
}

function validateNotEmpty() {
    VARIABLE_NAME=$1
    VARIABLE_VALUE=$2
#    echo "$VARIABLE_NAME: $VARIABLE_VALUE"
    if [ -z "$VARIABLE_VALUE" ]; then echo "$VARIABLE_NAME must contain at least one item."; exit 1; fi
}


### SCRIPT ###

cd "$(dirname "$0")"
. ./common.sh

cd $PROJECT_BUILD_DIRECTORY/docker-compose

PROJECT_NAME=performance


export DEPLOYMENT="${DEPLOYMENT:-singlenode}"
if isTestDeployment ; then loadProvisionedSystemProperties; fi
export OPERATION="${OPERATION:-provision}"

echo "DEPLOYMENT: $DEPLOYMENT"

case "$DEPLOYMENT" in
    singlenode) DOCKER_COMPOSE_FILE=docker-compose.yml ;;
    cluster) DOCKER_COMPOSE_FILE=docker-compose-cluster.yml ;;
    crossdc) DOCKER_COMPOSE_FILE=docker-compose-crossdc.yml ;;
    monitoring) DOCKER_COMPOSE_FILE=docker-compose-monitoring.yml ; DELETE_DATA="${DELETE_DATA:-false}" ;;
    *)
        echo "Deployment '$DEPLOYMENT' not supported by provisioner '$PROVISIONER'."
        exit 1
    ;;
esac


echo "OPERATION: $OPERATION"

case "$OPERATION" in

    provision)

        case "$DEPLOYMENT" in

            singlenode)
                BASE_SERVICES="mariadb"
                KEYCLOAK_SERVICES="keycloak"

                validateNotEmpty DB_CPUSETS $DB_CPUSETS
                DB_CPUSETS_ARRAY=( $DB_CPUSETS )
                export DB_CPUSET=${DB_CPUSETS_ARRAY[0]}

                validateNotEmpty KEYCLOAK_CPUSETS $KEYCLOAK_CPUSETS
                KEYCLOAK_CPUSETS_ARRAY=( $KEYCLOAK_CPUSETS )
                export KEYCLOAK_CPUSET=${KEYCLOAK_CPUSETS_ARRAY[0]}

                echo "DB_CPUSET: $DB_CPUSET"
                echo "KEYCLOAK_CPUSET: $KEYCLOAK_CPUSET"
                echo "BASE_SERVICES: $BASE_SERVICES"
                echo "KEYCLOAK_SERVICES: $KEYCLOAK_SERVICES"

            ;;

            cluster)
                BASE_SERVICES="mariadb loadbalancer"

                validateNotEmpty DB_CPUSETS $DB_CPUSETS
                DB_CPUSETS_ARRAY=( $DB_CPUSETS )
                export DB_CPUSET=${DB_CPUSETS_ARRAY[0]}

                validateNotEmpty LB_CPUSETS $LB_CPUSETS
                LB_CPUSETS_ARRAY=( $LB_CPUSETS )
                export LB_CPUSET=${LB_CPUSETS_ARRAY[0]}

                validateNotEmpty KEYCLOAK_CPUSETS $KEYCLOAK_CPUSETS
                KEYCLOAK_CPUSETS_ARRAY=( $KEYCLOAK_CPUSETS )

                KEYCLOAK_MAX_SCALE=${#KEYCLOAK_CPUSETS_ARRAY[@]}
                KEYCLOAK_SCALE="${KEYCLOAK_SCALE:-$KEYCLOAK_MAX_SCALE}"
                if [ $KEYCLOAK_SCALE -gt $KEYCLOAK_MAX_SCALE ]; then KEYCLOAK_SCALE=$KEYCLOAK_MAX_SCALE; fi
                if [ $KEYCLOAK_SCALE -lt 1 ]; then KEYCLOAK_SCALE=1; fi

                echo "DB_CPUSET: $DB_CPUSET"
                echo "LB_CPUSET: $LB_CPUSET"
                echo "KEYCLOAK_CPUSETS: $KEYCLOAK_CPUSETS"
                echo "KEYCLOAK_SCALE: ${KEYCLOAK_SCALE} (max ${KEYCLOAK_MAX_SCALE})"

                KEYCLOAK_SERVICES=""
                STOPPED_KEYCLOAK_SERVICES=""
                for ((i=1; i<=$KEYCLOAK_MAX_SCALE; i++)) ; do
                    if (( $i <= $KEYCLOAK_SCALE )) ; then
                        KEYCLOAK_SERVICES="$KEYCLOAK_SERVICES keycloak_$i"
                    else
                        STOPPED_KEYCLOAK_SERVICES="$STOPPED_KEYCLOAK_SERVICES keycloak_$i"
                    fi
                done
                echo "BASE_SERVICES: $BASE_SERVICES"
                echo "KEYCLOAK_SERVICES: $KEYCLOAK_SERVICES"
                
                generateDockerComposeFile
            ;;

            crossdc)
                BASE_SERVICES="mariadb_dc1 mariadb_dc2 infinispan_dc1 infinispan_dc2 loadbalancer_dc1 loadbalancer_dc2"

                validateNotEmpty DB_DC1_CPUSETS $DB_DC1_CPUSETS
                validateNotEmpty DB_DC2_CPUSETS $DB_DC2_CPUSETS
                DB_DC1_CPUSETS_ARRAY=( $DB_DC1_CPUSETS )
                DB_DC2_CPUSETS_ARRAY=( $DB_DC2_CPUSETS )
                export DB_DC1_CPUSET=${DB_DC1_CPUSETS_ARRAY[0]}
                export DB_DC2_CPUSET=${DB_DC2_CPUSETS_ARRAY[0]}
                echo "DB_DC1_CPUSET: $DB_DC1_CPUSET"
                echo "DB_DC2_CPUSET: $DB_DC2_CPUSET"

                validateNotEmpty LB_DC1_CPUSETS $LB_DC1_CPUSETS
                validateNotEmpty LB_DC2_CPUSETS $LB_DC2_CPUSETS
                LB_DC1_CPUSETS_ARRAY=( $LB_DC1_CPUSETS )
                LB_DC2_CPUSETS_ARRAY=( $LB_DC2_CPUSETS )
                export LB_DC1_CPUSET=${LB_DC1_CPUSETS_ARRAY[0]}
                export LB_DC2_CPUSET=${LB_DC2_CPUSETS_ARRAY[0]}
                echo "LB_DC1_CPUSET: $LB_DC1_CPUSET"
                echo "LB_DC2_CPUSET: $LB_DC2_CPUSET"

                validateNotEmpty INFINISPAN_DC1_CPUSETS $INFINISPAN_DC1_CPUSETS
                validateNotEmpty INFINISPAN_DC2_CPUSETS $INFINISPAN_DC2_CPUSETS
                INFINISPAN_DC1_CPUSETS_ARRAY=( $INFINISPAN_DC1_CPUSETS )
                INFINISPAN_DC2_CPUSETS_ARRAY=( $INFINISPAN_DC2_CPUSETS )
                export INFINISPAN_DC1_CPUSET=${INFINISPAN_DC1_CPUSETS_ARRAY[0]}
                export INFINISPAN_DC2_CPUSET=${INFINISPAN_DC2_CPUSETS_ARRAY[0]}
                echo "INFINISPAN_DC1_CPUSET: $INFINISPAN_DC1_CPUSET"
                echo "INFINISPAN_DC2_CPUSET: $INFINISPAN_DC2_CPUSET"

                validateNotEmpty KEYCLOAK_DC1_CPUSETS $KEYCLOAK_DC1_CPUSETS
                validateNotEmpty KEYCLOAK_DC2_CPUSETS $KEYCLOAK_DC2_CPUSETS
                KEYCLOAK_DC1_CPUSETS_ARRAY=( $KEYCLOAK_DC1_CPUSETS )
                KEYCLOAK_DC2_CPUSETS_ARRAY=( $KEYCLOAK_DC2_CPUSETS )
                KEYCLOAK_DC1_MAX_SCALE=${#KEYCLOAK_DC1_CPUSETS_ARRAY[@]}
                KEYCLOAK_DC2_MAX_SCALE=${#KEYCLOAK_DC2_CPUSETS_ARRAY[@]}
                KEYCLOAK_DC1_SCALE="${KEYCLOAK_DC1_SCALE:-$KEYCLOAK_DC1_MAX_SCALE}"
                KEYCLOAK_DC2_SCALE="${KEYCLOAK_DC2_SCALE:-$KEYCLOAK_DC2_MAX_SCALE}"

                if [ $KEYCLOAK_DC1_SCALE -gt $KEYCLOAK_DC1_MAX_SCALE ]; then KEYCLOAK_DC1_SCALE=$KEYCLOAK_DC1_MAX_SCALE; fi
                if [ $KEYCLOAK_DC1_SCALE -lt 1 ]; then KEYCLOAK_DC1_SCALE=1; fi
                if [ $KEYCLOAK_DC2_SCALE -gt $KEYCLOAK_DC2_MAX_SCALE ]; then KEYCLOAK_DC2_SCALE=$KEYCLOAK_DC2_MAX_SCALE; fi
                if [ $KEYCLOAK_DC2_SCALE -lt 1 ]; then KEYCLOAK_DC2_SCALE=1; fi

                echo "KEYCLOAK_DC1_CPUSETS: ${KEYCLOAK_DC1_CPUSETS}"
                echo "KEYCLOAK_DC2_CPUSETS: ${KEYCLOAK_DC2_CPUSETS}"
                echo "KEYCLOAK_DC1_SCALE: ${KEYCLOAK_DC1_SCALE} (max ${KEYCLOAK_DC1_MAX_SCALE})"
                echo "KEYCLOAK_DC2_SCALE: ${KEYCLOAK_DC2_SCALE} (max ${KEYCLOAK_DC2_MAX_SCALE})"

                KEYCLOAK_SERVICES=""
                STOPPED_KEYCLOAK_SERVICES=""
                for ((i=1; i<=$KEYCLOAK_DC1_MAX_SCALE; i++)) ; do
                    if (( $i <= $KEYCLOAK_DC1_SCALE )) ; then
                        KEYCLOAK_SERVICES="$KEYCLOAK_SERVICES keycloak_dc1_$i"
                    else
                        STOPPED_KEYCLOAK_SERVICES="$STOPPED_KEYCLOAK_SERVICES keycloak_dc1_$i"
                    fi
                done
                for ((i=1; i<=$KEYCLOAK_DC2_MAX_SCALE; i++)) ; do
                    if (( $i <= $KEYCLOAK_DC2_SCALE )) ; then
                        KEYCLOAK_SERVICES="$KEYCLOAK_SERVICES keycloak_dc2_$i"
                    else
                        STOPPED_KEYCLOAK_SERVICES="$STOPPED_KEYCLOAK_SERVICES keycloak_dc2_$i"
                    fi
                done
                echo "BASE_SERVICES: $BASE_SERVICES"
                echo "KEYCLOAK_SERVICES: $KEYCLOAK_SERVICES"

                generateDockerComposeFile

            ;;

            monitoring)

                validateNotEmpty MONITORING_CPUSETS "$MONITORING_CPUSETS"
                MONITORING_CPUSETS_ARRAY=( $MONITORING_CPUSETS )
                export MONITORING_CPUSET=${MONITORING_CPUSETS_ARRAY[0]}
                echo "MONITORING_CPUSET: $MONITORING_CPUSET"

            ;;

        esac

        runCommand "docker-compose -f $DOCKER_COMPOSE_FILE -p ${PROJECT_NAME} up -d --build $BASE_SERVICES $KEYCLOAK_SERVICES"
        if [ ! -z "$STOPPED_KEYCLOAK_SERVICES" ] ; then 
            echo "STOPPED_KEYCLOAK_SERVICES: $STOPPED_KEYCLOAK_SERVICES"
            runCommand "docker-compose -f $DOCKER_COMPOSE_FILE -p ${PROJECT_NAME} stop $STOPPED_KEYCLOAK_SERVICES"
        fi

        if isTestDeployment ; then 
            generateProvisionedSystemProperties; 
            $PROJECT_BASEDIR/healthcheck.sh
        fi

    ;;


    teardown)

        DELETE_DATA="${DELETE_DATA:-true}"
        echo "DELETE_DATA: $DELETE_DATA"
        if "$DELETE_DATA" ; then VOLUMES_ARG="-v"; else VOLUMES_ARG=""; fi

        runCommand "docker-compose -f $DOCKER_COMPOSE_FILE -p ${PROJECT_NAME} down $VOLUMES_ARG"

        if isTestDeployment ; then removeProvisionedSystemProperties; fi
    ;;


    export-dump|import-dump)
        
        loadProvisionedSystemProperties

        echo "KEYCLOAK_SERVICES: $KEYCLOAK_SERVICES"
        if [ -z "$KEYCLOAK_SERVICES" ]; then echo "Unable to load KEYCLOAK_SERVICES"; exit 1; fi

        case "$DEPLOYMENT" in
            singlenode|cluster) export DB_CONTAINER=${PROJECT_NAME}_mariadb_1 ;;
            crossdc) export DB_CONTAINER=${PROJECT_NAME}_mariadb_dc1_1 ;;
            *) echo "Deployment '$DEPLOYMENT' doesn't support operation '$OPERATION'." ; exit 1 ;;
        esac
        if [ ! -f "$DATASET_PROPERTIES_FILE" ]; then echo "Operation '$OPERATION' requires a valid DATASET_PROPERTIES_FILE parameter."; exit 1; fi
        DATASET_PROPERTIES_FILENAME=`basename $DATASET_PROPERTIES_FILE`
        DATASET=${DATASET_PROPERTIES_FILENAME%.properties}
        echo "DATASET_PROPERTIES_FILE: $DATASET_PROPERTIES_FILE"
        echo "DATASET: $DATASET"

        echo "Stopping Keycloak services."
        runCommand "docker-compose -f $DOCKER_COMPOSE_FILE -p ${PROJECT_NAME} stop $KEYCLOAK_SERVICES"

        cd `dirname $DATASET_PROPERTIES_FILE`
        case "$OPERATION" in
            export-dump)
                echo "Exporting $DATASET.sql."
                if docker exec $DB_CONTAINER /usr/bin/mysqldump -u root --password=root keycloak > $DATASET.sql ; then 
                    echo "Compressing $DATASET.sql."
                    gzip $DATASET.sql
                fi
            ;;
            import-dump) 
                DUMP_DOWNLOAD_SITE=${DUMP_DOWNLOAD_SITE:-https://downloads.jboss.org/keycloak-qe}
                if [ ! -f "$DATASET.sql.gz" ]; then 
                    echo "Downloading dump file: $DUMP_DOWNLOAD_SITE/$DATASET.sql.gz"
                    if ! curl -f -O $DUMP_DOWNLOAD_SITE/$DATASET.properties -O $DUMP_DOWNLOAD_SITE/$DATASET.sql.gz ; then
                        echo Download failed.
                        exit 1
                    fi
                fi
                echo "Importing $DATASET.sql.gz"
                set -o pipefail
                if ! gunzip -c $DATASET.sql.gz | docker exec -i $DB_CONTAINER /usr/bin/mysql -u root --password=root keycloak ; then
                    echo Import failed.
                    exit 1
                fi
            ;;
        esac
        cd $PROJECT_BUILD_DIRECTORY/docker-compose

        echo "Starting Keycloak services."
        runCommand "docker-compose -f $DOCKER_COMPOSE_FILE -p ${PROJECT_NAME} up -d --no-recreate $KEYCLOAK_SERVICES"
        # need to update mapped ports
        generateProvisionedSystemProperties

        $PROJECT_BASEDIR/healthcheck.sh

    ;;

    collect)
        TIMESTAMP=`date -u "+%Y-%m-%d_%T_%Z"`
        ARTIFACTS_DIR="${PROJECT_BUILD_DIRECTORY}/collected-artifacts/${DEPLOYMENT}_${TIMESTAMP}"
        SERVICES=`docker-compose -f $DOCKER_COMPOSE_FILE -p ${PROJECT_NAME} config --services`

        GNUPLOT_SCRIPTS_DIR="$PROJECT_BASEDIR/src/main/gnuplot/jstat"
        GNUPLOT_COMMON="$GNUPLOT_SCRIPTS_DIR/common.gp"

        echo "Collecting service logs."
        rm -rf ${ARTIFACTS_DIR}; mkdir -p ${ARTIFACTS_DIR}
        for SERVICE in ${SERVICES}; do 
            mkdir -p "${ARTIFACTS_DIR}/${SERVICE}"
            # log files & configs
            if [[ $SERVICE =~ .*keycloak.* ]]; then 
                docker cp "${PROJECT_NAME}_${SERVICE}_1:/opt/jboss/keycloak/standalone/configuration" "${ARTIFACTS_DIR}/${SERVICE}/configuration"
                docker cp "${PROJECT_NAME}_${SERVICE}_1:/opt/jboss/keycloak/standalone/log" "${ARTIFACTS_DIR}/${SERVICE}/log"
            elif [[ $SERVICE =~ .*infinispan.* ]]; then 
                docker cp "${PROJECT_NAME}_${SERVICE}_1:/opt/jboss/infinispan-server/standalone/configuration" "${ARTIFACTS_DIR}/${SERVICE}/configuration"
                docker cp "${PROJECT_NAME}_${SERVICE}_1:/opt/jboss/infinispan-server/standalone/log" "${ARTIFACTS_DIR}/${SERVICE}/log"
            elif [[ $SERVICE =~ .*loadbalancer.* ]]; then 
                docker cp "${PROJECT_NAME}_${SERVICE}_1:/opt/jboss/wildfly/standalone/configuration" "${ARTIFACTS_DIR}/${SERVICE}/configuration"
                docker cp "${PROJECT_NAME}_${SERVICE}_1:/opt/jboss/wildfly/standalone/log" "${ARTIFACTS_DIR}/${SERVICE}/log"
            else
                docker logs "${PROJECT_NAME}_${SERVICE}_1" > ${ARTIFACTS_DIR}/${SERVICE}/docker.log 2>&1; 
                if [[ $? != 0 ]]; then echo "ERROR collecting from: ${SERVICE}"; rm ${ARTIFACTS_DIR}/${SERVICE}/docker.log; fi
            fi

#             jstat charts
            if ${JSTAT:-false}; then
                JSTAT_DATAFILE="${ARTIFACTS_DIR}/${SERVICE}/log/jstat-gc.log"
                if [ -f "$JSTAT_DATAFILE" ] && ${GNUPLOT:-false}; then
                    mkdir -p "${ARTIFACTS_DIR}/${SERVICE}/jstat-charts"
                    HTML="${ARTIFACTS_DIR}/${SERVICE}/jstat-charts/index.html"
                    echo "<html><head><title>JStat Charts for $SERVICE</title>" > "$HTML"
                    echo "<style>div.box{ display: -webkit-inline-box }</style></head>" >> "$HTML"
                    echo "<body><h1>JStat Charts for $SERVICE</h1>" >> "$HTML"
                    for GP_SCRIPT in gc-all gc-s0 gc-s1 gc-e gc-o gc-m gc-cc ; do
                        gnuplot -e "datafile='$JSTAT_DATAFILE'" "$GNUPLOT_COMMON" "$GNUPLOT_SCRIPTS_DIR/${GP_SCRIPT}.gp" > "${ARTIFACTS_DIR}/${SERVICE}/jstat-charts/${GP_SCRIPT}.png"
                        if [ $? == 0 ]; then 
                            echo "<div class='box'>" >> "$HTML"
                            echo "<b>${GP_SCRIPT}</b><br/>" >> "$HTML"
                            echo "<a href='${GP_SCRIPT}.png'><img src='${GP_SCRIPT}.png' width='400' height='300'/></a>" >> "$HTML"
                            echo "</div>" >> "$HTML"
                        fi
                    done
                    echo "</body></html>" >> "$HTML"
                fi
            fi
        done
        if [ -z "$(ls -A ${ARTIFACTS_DIR})" ]; then echo "No logs were collected."; rm -rf ${ARTIFACTS_DIR}; fi
    ;;

    *)
        echo "Unsupported operation: '$OPERATION'"
        exit 1
    ;;

esac

