#!/bin/bash

export MYHOST=node$(echo $MYSQL_NAME | awk -F"/dockercluster[^0-9]*|\/mysql" '{print  $2 }');
echo "MYHOST is $MYHOST. MYSQL_NAME is $MYSQL_NAME";

function waitForPreviousNodeStart
{
  myHostNumber=$(echo $MYHOST | awk -F"node" '{ print $2 }');
  if [ $myHostNumber -eq 1 ]; then
    echo "Our host is node1. No need to wait for previous server";
  else 
    previous=node$(($myHostNumber-1));
    echo "Waiting for host $previous to start";
    
    for I in $(seq 1 10); do
      cat /keycloak-docker-shared/keycloak-wildfly-$previous/standalone/log/server.log | grep "\(INFO\|ERROR\).*WildFly.*started";
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

echo "Creating keycloak-wildfly-$MYHOST";

cd /opt/wildfly
cp -r /keycloak-docker-cluster/modules ./

# Deploy keycloak
cp -r /keycloak-docker-cluster/deployments/* /opt/wildfly/standalone/deployments/

# Deploy and configure examples
/deploy-examples.sh

# Deploy to volume
rm -rf /keycloak-docker-shared/keycloak-wildfly-$MYHOST
cp -r /opt/wildfly-8.1.0.Final /keycloak-docker-shared/keycloak-wildfly-$MYHOST
chmod -R 777 /keycloak-docker-shared/keycloak-wildfly-$MYHOST
echo "keycloak-wildfly-$MYHOST prepared and copyied to volume";


waitForPreviousNodeStart;
waitForMySQLStart;

echo "Running keycloak node $MYHOST. Additional arguments: $@";
cd /keycloak-docker-shared
export JBOSS_HOME=/keycloak-docker-shared/keycloak-wildfly-$MYHOST;

cd $JBOSS_HOME/bin/

./standalone.sh -c standalone-ha.xml -Djboss.node.name=$MYHOST -b `hostname -i` -Djboss.mod_cluster.jvmRoute=$MYHOST \
-Dmysql.host=$MYSQL_PORT_3306_TCP_ADDR -Dhttpd.proxyList=$HTTPD_1_PORT_10001_TCP_ADDR:$HTTPD_PORT_10001_TCP_PORT \
-Dkeycloak.import=/keycloak-docker-cluster/examples/testrealm.json "$@"
