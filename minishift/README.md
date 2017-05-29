# Keycloak - Openshift commands

To deploy a Keycloak cluster in minishift use the following commands:

```
oc new-project keycloak --display-name="Keycloak server" \
--description="keycloak server + postgres"

oc new-app -f postgresql.json
sleep 20

# deploying 3 keycloak instances
oc new-app -f keycloak.json
```

### Customization options

#### KeyCloak

edit environment variables:

                "env":[
                  {
                    "name":"KEYCLOAK_USER",
                    "value":"admin"
                  },
                  {
                    "name":"KEYCLOAK_PASSWORD",
                    "value":"admin"
                  },
                  {
                    "name":"POSTGRES_DATABASE",
                    "value":"userdb"
                  },
                  {
                    "name":"POSTGRES_USER",
                    "value":"keycloak"
                  },
                  {
                    "name":"POSTGRES_PASSWORD",
                    "value":"password"
                  },
                  {
                    "name":"POSTGRES_PORT_5432_TCP_ADDR",
                    "value":"postgres"
                  },
                  {
                    "name":"POSTGRES_PORT_5432_TCP_PORT",
                    "value":"5432"
                  },
                  {
                    "name":"OPERATING_MODE",
                    "value":"clustered"
                  }
                ]


#### Postgresql

            "env": [
              {
                "name": "POSTGRESQL_USER",
                "value": "keycloak"
              },
              {
                "name": "POSTGRESQL_PASSWORD",
                "value": "password"
              },
              {
                "name": "POSTGRESQL_DATABASE",
                "value": "userdb"
              },
              {
                "name": "POSTGRESQL_ADMIN_PASSWORD",
                "value": "password"
              }
            ]
