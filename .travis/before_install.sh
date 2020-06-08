#!/bin/bash
set -ex;

if test ${TRAVIS_ARCH} = "s390x";
then
  sudo apt-get update
  sudo apt-get install -y phantomjs maven
  curl -fsSL https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u252-b09_openj9-0.20.0/OpenJDK8U-jdk_s390x_linux_openj9_8u252b09_openj9-0.20.0.tar.gz | tar xz
else
  export PHANTOMJS_VERSION=2.1.1;
  phantomjs --version;
  export PATH=$PWD/travis_phantomjs/phantomjs-$PHANTOMJS_VERSION-linux-x86_64/bin:$PATH;
  phantomjs --version;
  if [ $(phantomjs --version) != $PHANTOMJS_VERSION ]; then 
    rm -rf $PWD/travis_phantomjs; mkdir -p $PWD/travis_phantomjs;
    wget https://github.com/Medium/phantomjs/releases/download/v$PHANTOMJS_VERSION/phantomjs-$PHANTOMJS_VERSION-linux-x86_64.tar.bz2 -O $PWD/travis_phantomjs/phantomjs-$PHANTOMJS_VERSION-linux-x86_64.tar.bz2;
    tar -xvf $PWD/travis_phantomjs/phantomjs-$PHANTOMJS_VERSION-linux-x86_64.tar.bz2 -C $PWD/travis_phantomjs; 
  fi;
  phantomjs --version;
fi
