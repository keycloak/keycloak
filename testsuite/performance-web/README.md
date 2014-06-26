Adding users
------------

Adding 1000 new users (will start from last added user, so you don't need to explicitly check how many users to create are needed: 
http://localhost:8081/keycloak-tools/perf/perf-realm/create-available-users?prefix=user&count=1000&batch=100&roles=user

Checking users count: 
http://localhost:8081/keycloak-tools/perf/perf-realm/get-users-count?prefix=user

Switching to Mongo
------------------
Start with: (TODO)
-Dkeycloak.model.provider=mongo -Dkeycloak.model.mongo.db=keycloak-perf