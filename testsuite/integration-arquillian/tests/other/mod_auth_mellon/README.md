# Mod_auth_mellon test

## Docker images

Each docker image contains apache + mod_auth_mellon, ssh daemon and two html files unprotected (/) and protected (/auth).
SSH in each docker uses root user with password 'a' (without quotes).

## Build docker images

docker build -t apache-mellon docker/
docker build -t apache-mellon2 docker2/

## Run docker image

docker run -d -p 8380:80 -p 8322:22 apache-mellon
docker run -d -p 8480:80 -p 8322:22 apache-mellon2

## Run tests

mvn clean install [-Dapache.mod_auth_mellon.url=http://localhost:8380 -Dapache.mod_auth_mellon2.url=http://localhost:8480]