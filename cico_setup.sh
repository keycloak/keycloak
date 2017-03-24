#!/bin/bash
# Output command before executing
set -x

# Exit on error
set -e

REPO_NAME="almighty"
CURRENT_DIR=$(pwd)
PROJECT_NAME="keycloak"
DOCKER_IMAGE_CORE=$PROJECT_NAME
DOCKER_IMAGE_DEPLOY=$PROJECT_NAME-deploy

KEYCLOAK_VERSION="3.0.0.Final"

# Source environment variables of the jenkins slave
# that might interest this worker.
function load_jenkins_vars() {
  if [ -e "jenkins-env" ]; then
    cat jenkins-env \
      | grep -E "(JENKINS_URL|GIT_BRANCH|GIT_COMMIT|BUILD_NUMBER|ghprbSourceBranch|ghprbActualCommit|BUILD_URL|ghprbPullId)=" \
      | sed 's/^/export /g' \
      > ~/.jenkins-env
    source ~/.jenkins-env
  fi
}

function install_deps() {
  # We need to disable selinux for now, XXX
  /usr/sbin/setenforce 0

  # Get all the deps in
  yum -y install \
    docker \
    make \
    git \
    wget \
    curl

  wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
  yum -y install apache-maven

  service docker start
  echo 'CICO: Dependencies installed'
}

function build() {
  # Set the version according to the ENV variable
  #mvn -q versions:set -DgenerateBackupPoms=false -DnewVersion=$KEYCLOAK_VERSION
  # Only build the keycloak-server to save time
  #echo 'CICO: Installing without specifying a version'
  #mvn clean install -DskipTests=true -pl :keycloak-server-dist -am -P distribution
  echo 'CICO: Run mv clean install -DskipTests=true -Pdistribution'
  mvn clean install -DskipTests=true -Pdistribution

  echo 'CICO: Listing the directory server-dist'
  ls distribution/server-dist/
  echo 'CICO: Listing the directory target'
  ls distribution/server-dist/target
  echo 'CICO: keycloak-server build completed successfully!'
}

function deploy() {
  cp distribution/server-dist/target/keycloak-$KEYCLOAK_VERSION.tar.gz docker

  # Let's deploy
  docker build -t $DOCKER_IMAGE_DEPLOY -f $CURRENT_DIR/docker/Dockerfile $CURRENT_DIR/docker

  rm docker/keycloak-$KEYCLOAK_VERSION.tar.gz

  docker tag $DOCKER_IMAGE_DEPLOY registry.devshift.net/$REPO_NAME/$PROJECT_NAME-postgres:latest
  docker push registry.devshift.net/$REPO_NAME/$PROJECT_NAME-postgres:latest
  echo 'CICO: Image pushed, ready to update deployed app'
}

function cico_setup() {
  load_jenkins_vars;
  install_deps;
  build;
}
