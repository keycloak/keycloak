# Keycloak Image
For more information, see the [Running Keycloak in a container guide](https://www.keycloak.org/server/containers).

## Build the image

It is possible to download the Keycloak distribution from a URL:

    docker build --build-arg KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/<VERSION>/keycloak-<VERSION>.tar.gz . -t <YOUR_TAG>

Alternatively, you need to build the local distribution first, then copy the distributions tar package in the `containers` folder and point the build command to use the image:

    cp $KEYCLOAK_SOURCE/quarkus/dist/target/keycloak-<VERSION>.tar.gz .
    docker build --build-arg KEYCLOAK_DIST=keycloak-<VERSION>.tar.gz . -t <YOUR_TAG>

### Build a image based on alpine

To build an image based on alpine linux you must add a build argument `--build-arg DIST=alpine`.

```
docker build \
    --build-arg DIST=alpine \
    --build-arg KEYCLOAK_DIST=https://github.com/keycloak/keycloak/releases/download/<VERSION>/keycloak-<VERSION>.tar.gz . \
     -t <YOUR_TAG>
```
