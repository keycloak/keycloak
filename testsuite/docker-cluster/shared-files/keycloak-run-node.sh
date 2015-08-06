#!/bin/bash

export MYHOST="$NODE_PREFIX"node$(echo $MYSQL_NAME | awk -F"/dockercluster[^0-9]*|\/mysql" '{print  $2 }');
echo "MYHOST is $MYHOST. MYSQL_NAME is $MYSQL_NAME";

function prepareHost
{
  if [ -d /keycloak-docker-shared/keycloak-$JBOSS_TYPE-$MYHOST ]; then
    echo "Node $MYHOST already prepared. Skiping";
    return;
  fi

  echo "Creating keycloak-$JBOSS_TYPE-$MYHOST";

  /keycloak-docker-cluster/shared-files/keycloak-base-prepare.sh

  echo "Base prepare finished";

  # Deploy to volume
  rm -rf /keycloak-docker-shared/keycloak-$JBOSS_TYPE-$MYHOST
  cp -r $JBOSS_HOME /keycloak-docker-shared/keycloak-$JBOSS_TYPE-$MYHOST
  chmod -R 777 /keycloak-docker-shared/keycloak-$JBOSS_TYPE-$MYHOST
  echo "keycloak-$JBOSS_TYPE-$MYHOST prepared and copyied to volume";
}

function waitForPreviousNodeStart
{
  myHostNumber=$(echo $MYHOST | awk -F"node" '{ print $2 }');
  if [ $myHostNumber -eq 1 ]; then
    echo "Our host is $MYHOST. No need to wait for previous server";
  else 
    previous="$NODE_PREFIX"node$(($myHostNumber-1));
    echo "Waiting for host $previous to start";
    
    for I in $(seq 1 10); do
      cat /keycloak-docker-shared/keycloak-$JBOSS_TYPE-$previous/standalone/log/server.log | grep "\(INFO\|ERROR\).*\(WildFly\|JBoss AS\|JBoss EAP\).*started";
      if [ 0 -eq $? ]; then
        echo "Host $previous started. Going to start $MYHOST";
        return;
      fi;
      
      echo "Host $previous not started yet. Still waiting...";      
      sleep 5;
    done; 

    echo "Host $previous not started yet within timeout.";
  fi;      
}

function waitForMySQLStart
{
  for I in $(seq 1 10); do
    nc $MYSQL_PORT_3306_TCP_ADDR 3306 < /dev/null;
    mysqlRunning=$(echo $?);
    if [ $mysqlRunning -eq 0 ]; then
      echo "MySQL is running. Starting our server";
      return;
    else
      echo "MySQL not yet available. Still waiting...";
      sleep 5;
    fi;
  done;
}

prepareHost;

waitForPreviousNodeStart;
waitForMySQLStart;

echo "Running keycloak node $MYHOST. Additional arguments: $@";
cd /keycloak-docker-shared
export JBOSS_HOME=/keycloak-docker-shared/keycloak-$JBOSS_TYPE-$MYHOST;

cd $JBOSS_HOME/bin/

./standalone.sh -c standalone-keycloak-ha.xml -Djboss.node.name=$MYHOST -b `hostname -i` -Djboss.mod_cluster.jvmRoute=$MYHOST \
-Dmysql.host=$MYSQL_PORT_3306_TCP_ADDR -Dhttpd.proxyHost=$HTTPD_1_PORT_10001_TCP_ADDR -Dhttpd.proxyPort=$HTTPD_PORT_10001_TCP_PORT \
-Dkeycloak.import=/keycloak-docker-cluster/examples/testrealm.json "$@"
