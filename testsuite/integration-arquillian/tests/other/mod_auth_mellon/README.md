# Mod_auth_mellon test

## Docker image

Docker image contains apache + mod_auth_mellon and two html files unprotected (/) and protected (/auth).
 
## Build docker image

docker build -t apache-mellon docker/

## Run docker image

docker run -d -p 8380:80 apache-mellon

## Run tests

mvn clean install [-Dapache.mod_auth_mellon.url=http://localhost:8380]