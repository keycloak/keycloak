/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.model.parameters;

import com.google.common.collect.ImmutableSet;
import org.jboss.logging.Logger;
import org.keycloak.authorization.store.StoreFactorySpi;
import org.keycloak.events.EventStoreSpi;
import org.keycloak.models.DeploymentStateSpi;
import org.keycloak.models.SingleUseObjectSpi;
import org.keycloak.models.UserLoginFailureSpi;
import org.keycloak.models.UserSessionSpi;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionProviderFactory;
import org.keycloak.models.map.authorization.MapAuthorizationStoreFactory;
import org.keycloak.models.map.client.MapClientProviderFactory;
import org.keycloak.models.map.clientscope.MapClientScopeProviderFactory;
import org.keycloak.models.map.deploymentState.MapDeploymentStateProviderFactory;
import org.keycloak.models.map.events.MapEventStoreProviderFactory;
import org.keycloak.models.map.group.MapGroupProviderFactory;
import org.keycloak.models.map.keys.MapPublicKeyStorageProviderFactory;
import org.keycloak.models.map.loginFailure.MapUserLoginFailureProviderFactory;
import org.keycloak.models.map.realm.MapRealmProviderFactory;
import org.keycloak.models.map.role.MapRoleProviderFactory;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectProviderFactory;
import org.keycloak.models.map.storage.MapStorageSpi;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory;
import org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory;
import org.keycloak.models.map.storage.jpa.liquibase.connection.MapLiquibaseConnectionProviderFactory;
import org.keycloak.models.map.storage.jpa.liquibase.connection.MapLiquibaseConnectionSpi;
import org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterProviderFactory;
import org.keycloak.models.map.storage.jpa.updater.MapJpaUpdaterSpi;
import org.keycloak.models.map.user.MapUserProviderFactory;
import org.keycloak.models.map.userSession.MapUserSessionProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.sessions.AuthenticationSessionSpi;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

public class JpaMapStorageCockroachdb extends KeycloakModelParameters {

    private static final Logger LOG = Logger.getLogger(JpaMapStorageCockroachdb.class.getName());

    private static final Boolean START_CONTAINER = Boolean.valueOf(System.getProperty("cockroachdb.start-container", "true"));
    private static final String COCKROACHDB_DOCKER_IMAGE_NAME = System.getProperty("keycloak.map.storage.cockroachdb.docker.image", "cockroachdb/cockroach:v22.1.0");
    private static final CockroachContainer COCKROACHDB_CONTAINER = new CockroachContainer(DockerImageName.parse(COCKROACHDB_DOCKER_IMAGE_NAME).asCompatibleSubstituteFor("cockroachdb"));
    private static final String COCKROACHDB_DB_USER = System.getProperty("keycloak.map.storage.connectionsJpa.user", "keycloak");
    private static final String COCKROACHDB_DB_PASSWORD = System.getProperty("keycloak.map.storage.connectionsJpa.password", "pass");

    private static String COCKROACHDB_DB_JDBC_URL = System.getProperty("keycloak.map.storage.connectionsJpa.url");

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
            .add(MapJpaUpdaterSpi.class)
            .add(MapLiquibaseConnectionSpi.class)
            .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
            .add(ConcurrentHashMapStorageProviderFactory.class)
            .add(JpaMapStorageProviderFactory.class)
            .add(MapJpaUpdaterProviderFactory.class)
            .add(MapLiquibaseConnectionProviderFactory.class)
            .build();

    public JpaMapStorageCockroachdb() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

    @Override
    public void updateConfig(Config cf) {
        cf.spi(MapStorageSpi.NAME)
                .provider(ConcurrentHashMapStorageProviderFactory.PROVIDER_ID)
                .config("dir", "${project.build.directory:target}");

        cf.spi(MapStorageSpi.NAME)
                .provider(JpaMapStorageProviderFactory.PROVIDER_ID)
                .config("url", COCKROACHDB_DB_JDBC_URL)
                .config("user", COCKROACHDB_DB_USER)
                .config("password", COCKROACHDB_DB_PASSWORD)
                .config("driver", "org.postgresql.Driver")
                .config("driverDialect", "org.keycloak.models.map.storage.jpa.hibernate.dialect.JsonbPostgreSQL95Dialect");

        cf.spi(AuthenticationSessionSpi.PROVIDER_ID).provider(MapRootAuthenticationSessionProviderFactory.PROVIDER_ID)  .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("client").provider(MapClientProviderFactory.PROVIDER_ID)                                                 .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("clientScope").provider(MapClientScopeProviderFactory.PROVIDER_ID)                                       .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("group").provider(MapGroupProviderFactory.PROVIDER_ID)                                                   .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("realm").provider(MapRealmProviderFactory.PROVIDER_ID)                                                   .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("role").provider(MapRoleProviderFactory.PROVIDER_ID)                                                     .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi(DeploymentStateSpi.NAME).provider(MapDeploymentStateProviderFactory.PROVIDER_ID)                         .config(STORAGE_CONFIG, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID)
          .spi(StoreFactorySpi.NAME).provider(MapAuthorizationStoreFactory.PROVIDER_ID)                                 .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("user").provider(MapUserProviderFactory.PROVIDER_ID)                                                     .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi(UserLoginFailureSpi.NAME).provider(MapUserLoginFailureProviderFactory.PROVIDER_ID)                       .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi(SingleUseObjectSpi.NAME).provider(MapSingleUseObjectProviderFactory.PROVIDER_ID)                         .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi("publicKeyStorage").provider(MapPublicKeyStorageProviderFactory.PROVIDER_ID)                             .config(STORAGE_CONFIG, ConcurrentHashMapStorageProviderFactory.PROVIDER_ID)
          .spi(UserSessionSpi.NAME).provider(MapUserSessionProviderFactory.PROVIDER_ID)                                 .config(STORAGE_CONFIG, JpaMapStorageProviderFactory.PROVIDER_ID)
          .spi(EventStoreSpi.NAME).provider(MapEventStoreProviderFactory.PROVIDER_ID)                                   .config("storage-admin-events.provider", JpaMapStorageProviderFactory.PROVIDER_ID)
                                                                                                                        .config("storage-auth-events.provider", JpaMapStorageProviderFactory.PROVIDER_ID);
    }

    @Override
    public void beforeSuite(Config cf) {
        if (START_CONTAINER) {
            COCKROACHDB_CONTAINER
                    // Using the environment variables for now where using the withXXX() method is not supported, yet.
                    // https://github.com/testcontainers/testcontainers-java/issues/6299
                    .withEnv("COCKROACH_DATABASE", "keycloak")
                    .withEnv("COCKROACH_USER", COCKROACHDB_DB_USER)
                    // password is not used/supported in insecure mode
                    .withCommand("start-single-node", "--insecure")
                    .start();

            COCKROACHDB_DB_JDBC_URL = COCKROACHDB_CONTAINER.getJdbcUrl();
        }
    }

    @Override
    public void afterSuite() {
        if (START_CONTAINER) {
            COCKROACHDB_CONTAINER.stop();
        }
    }
}
