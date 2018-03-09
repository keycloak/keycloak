# Keycloak Performance Testsuite - Generating datasets


## Generating a set of datasets for multiple realms

The first dataset is small and is created quickly. Building of each subsequent dataset continues on top
of the previous dataset.

Datasets are created with a specific released server version (rather than a snapshot) in order to be
usable with later releases - newer server version should be able to migrate schema from any previous release.

We use 10 concurrent threads, which is enough to saturate a 
dual core machine. For quad-core you can try to double the number of workers.

```
cd testsuite/performance

mvn clean install -Dserver.version=4.0.0.Beta1

mvn verify -Pteardown
mvn verify -Pprovision
mvn verify -Pgenerate-data -Ddataset=10r100u1c -DnumOfWorkers=10
mvn verify -Pexport-dump -Ddataset=10r100u1c

mvn verify -Pgenerate-data -Ddataset=20r100u1c -DstartAtRealmIdx=10 -DnumOfWorkers=10
mvn verify -Pexport-dump -Ddataset=20r100u1c

mvn verify -Pgenerate-data -Ddataset=50r100u1c -DstartAtRealmIdx=20 -DnumOfWorkers=10
mvn verify -Pexport-dump -Ddataset=50r100u1c

mvn verify -Pgenerate-data -Ddataset=200r100u1c -DstartAtRealmIdx=50 -DnumOfWorkers=10
mvn verify -Pexport-dump -Ddataset=200r100u1c

mvn verify -Pgenerate-data -Ddataset=500r100u1c -DstartAtRealmIdx=200 -DnumOfWorkers=10
mvn verify -Pexport-dump -Ddataset=500r100u1c
```

If the dataset dump file is not available locally but it's known that the dataset for specific version exists on the server
it can be retrieved by specifying a proper server version again. For example:
```
mvn verify -Pteardown
mvn clean install
mvn verify -Pprovision
mvn verify -Pimport-dump -Ddataset=20r100u1c -Dserver.version=4.0.0.Beta1

```
