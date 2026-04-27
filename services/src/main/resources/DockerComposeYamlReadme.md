# Docker Compose YAML Installation
-----------------------------------

*NOTE:* This installation method is intended for development use only.  Please don't ever let this anywhere near prod!

## Keycloak Realm Assumptions:
 - Client configuration has not changed since the installation files were generated.  If you change your client configuration, be sure to grab a re-generated installation .zip from the 'Installation' tab.
 - Keycloak server is started with the 'docker' feature enabled.  I.E. -Dkeycloak.profile.feature.docker=enabled

## Running the Installation:
 - Spin up a fully functional docker registry with:
 
    docker-compose up
    
 - Now you can login against the registry and perform normal operations:
 
    docker login -u $username -p $password localhost:5000
    
    docker pull centos:7
    docker tag centos:7 localhost:5000/centos:7
    docker push localhost:5000/centos:7
    
 ** Remember that users for the `docker login` command must be configured and available in the keycloak realm that hosts the docker client.