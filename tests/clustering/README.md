# Running clustering tests

## Mixed cluster

KC_TEST_SERVER_IMAGES   -> if empty, uses the built distribution archive from quarkus/dist directory in all containers
-> if single value, uses that value in all the containers
-> if comma separated value ("imageA,imageB"), each container will use the image specified from the list. The number of items must match the cluster size.
-> "-" special keyword to use the built distribution archive.
> NOTE: If testing SNAPSHOT versions with "-", it's necessary for it to appear later in the CSV list than
> non-SNAPSHOT releases in order to avoid "Incorrect state of migration" exceptions.

KC_TEST_SERVER=cluster  -> enables cluster mode (configured by default in clustering module)
KC_TEST_DATABASE_INTERNAL=true -> configure keycloak with the internal database container IP instead of localhost (configured by default in clustering module)

Example, 2 node cluster, the first using the distribution archive and the second the nightly image
KC_TEST_DATABASE=postgres KC_TEST_SERVER_IMAGES=-,quay.io/keycloak/keycloak:nightly mvn verify -pl tests/clustering/  -Dtest=MixedVersionClusterTest

Using a mixed cluster with 26.2.3 and 26.2.4
KC_TEST_DATABASE=postgres KC_TEST_SERVER_IMAGES=quay.io/keycloak/keycloak:26.2.3,quay.io/keycloak/keycloak:26.2.4 mvn verify -pl tests/clustering/  -Dtest=MixedVersionClusterTest

The test has some println to check the state. Example:

```
url0->http://localhost:32889
url1->http://localhost:32891
```