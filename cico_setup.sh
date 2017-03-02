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

KEYCLOAK_VERSION="3.0.0.CR1-SNAPSHOT"

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
  mvn clean install -DskipTests=true -Pdistribution
}

function deploy() {
  cp distribution/server-dist/target/keycloak-$KEYCLOAK_VERSION.tar.gz docker

  # Let's deploy
  docker build -t $DOCKER_IMAGE_DEPLOY -f $CURRENT_DIR/docker/Dockerfile $CURRENT_DIR/docker

  rm docker/keycloak-$KEYCLOAK_VERSION.tar.gz
	
  docker tag $DOCKER_IMAGE_DEPLOY 8.43.84.245.xip.io/$REPO_NAME/$PROJECT_NAME:latest
  docker push 8.43.84.245.xip.io/$REPO_NAME/$PROJECT_NAME:latest
  echo 'CICO: Image pushed, ready to update deployed app'
}

function cico_setup() {
  load_jenkins_vars;
  install_deps;
  build;
}
