# Keycloak Datasets

## Provision Keycloak Server

Before generating data it is necessary to provision/start Keycloak server. This can 
be done automatically by running:

```
cd testsuite/performance
mvn clean install
mvn verify -P provision
```
To tear down the system after testing run:
```
mvn verify -P teardown
```
The teardown step will delete the database as well so it is possible to use it between generating different datasets.

It is also possible to start the server externally (manually). In that case it is necessary
to provide information in file `tests/target/provisioned-system.properties`.
See the main README for details.

## Generate Data

To generate the *default dataset* run: 
```
cd testsuite/performance
mvn verify -P generate-data
```

To generate a *specific dataset* from within the project run:
```
mvn verify -P generate-data -Ddataset=<NAMED_DATASET>
```
This will load dataset properties from `tests/src/test/resources/dataset/${dataset}.properties`.

To generate a specific dataset from a *custom properties file* run:
```
mvn verify -P generate-data -Ddataset.properties.file=<FULL_PATH_TO_PROPERTIES_FILE>
```

To delete a dataset run:
```
mvn verify -P generate-data -Ddataset=… -Ddelete=true
```
This will delete all realms specified by the dataset.


## Indexed Model

The model is hierarchical with the parent-child relationships determined by primary foreign keys of entities.

Size of the dataset is determined by specifying a "count per parent" parameter for each entity.

Number of mappings between entities created by the primary "count per parent" parameters
can be speicied by "count per other entity" parameters.

Each nested entity has a unique index which identifies it inside its parent entity.

For example:
- Realm X --> Client Y --> Client Role Z
- Realm X --> Client Y --> Resource Server --> Resource Z
- Realm X --> User Y
- etc.

Hash code of each entity is computed based on its index coordinates within the model and its class name.

Each entity holds entity representation, and a list of mappings to other entities in the indexed model.
The attributes and mappings are initialized by a related *entity template* class.
Each entity class also acts as a wrapper around a Keycloak Admin Client using it 
to provide CRUD operations for its entity.

The `id` attribute in the entity representation is set upon entity creation, or in case 
an already initialized entity was removed from LRU cache it is reloaded from the server.
This may happen if the number of entities is larger than entity cache size. (see below)

### Attribute Templating

Attributes of each supported entity representation can be set via FreeMarker templates.
The process is based on templates defined in a properties configuration file.

The first template in the list can use the `index` of the entity and any attributes of its parent entity.
Each subsequent attribute template can use any previously set attribute values.

Note: Output of FreeMarker engine is always a String. Transition to the actual type 
of the attribute is done with the Jackson 2.9+ parser using `ObjectMapper.update()` method
which allows a gradual updates of an existing Java object.

### Randomness

Randomness in the indexed model is deterministic (pseudorandom) because the 
random seeds are based on deterministic hash codes.

There are 2 types of seeds: one is for using randoms in the FreeMarker templates 
via methods `indexBasedRandomInt(int bound)` and `indexBasedRandomBool(int percentage)`.
It is based on class of the current entity + hash code of its parent entity.

The other seed is for generating mappings to other entities which are just 
random sequences of integer indexes. This is based on hash code of the current entity.

### Generator Settings

#### Timeouts
- `queue.timeout`: How long to wait for an entity to be processed by a thread-pool executor. Default is `60` seconds. 
You might want to increase this setting when deleting many realms with many nested entities using a low number of workers.
- `shutdown.timeout`: How long to wait for the executor thread-pool to shut down. Default is `60` seconds.

#### Caching and Memory
- `template.cache.size`: Size of cache of FreeMarker template models. Default is `10000`.
- `randoms.cache.size`: Size of cache of random integer sequences which are used for mappings between entities. Default is `10000`.
- `entity.cache.size`: Size of cache of initialized entities. Default is `100000`.
- `max.heap`: Max heap size of the data generator JVM.


## Notes:

- Mappings are random so it can sometimes happen that the same mappings are generated multiple times.
Only distinct mappings are created.
This means for example that if you specify `realmRolesPerUser=5` it can happen 
that only 4 or less roles will be actually mapped.

    There is an option to use unique random sequences but is is disabled right now 
because checking for uniqueness is CPU-intensive.

- Mapping of client roles to a user right now is determined by a single parameter: `clientRolesPerUser`. 

    Actually created mappings -- each of which contains specific client + a set of its roles -- is created 
based on the list of randomly selected client roles of all clients in the realm. 
This means the count of the actual client mappings isn't predictable. 

    That would require specifying 2 parameters: `clientsPerUser` and `clientRolesPerClientPerUser`
which would say how many clients a user has roles assigned from, and the number of roles per each of these clients.

- Number of resource servers depends on how the attribute `authorizationServicesEnabled` 
is set for each client. This means the number isn't specified  by any "perRealm" parameter. 
If this is needed it can be implemented via a random mapping from a resource server entity 
to a set of existing clients in a similar fashion to how a resource is selected for each resource permission.

- The "resource type" attribute for each resource and resource-based permission defaults to 
the default type of the parent resource server. 
If it's needed a separate abstract/non-persistable entity ResourceType can be created in the model
to represent a set of resource types. The "resource type" attributes can then be set based on random mappings into this set.

- Generating large number of users can take a long time with the default realm settings
which have the password hashing iterations set to a default value of 27500.
If you wish to speed this process up decrease the value of `hashIterations()` in attribute `realm.passwordPolicy`.

    Note that this will also significantly affect the performance results of the tests because 
password hashing takes a major part of the server's compute resources. The results may
improve even by a factor of 10 or higher when the hashing is set to the minimum value of 1 itreration.
However it's on the expense of security.

