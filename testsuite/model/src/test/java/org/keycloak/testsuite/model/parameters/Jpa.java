/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.util.Set;

import org.keycloak.authorization.jpa.store.JPAAuthorizationStoreFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderSpi;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionSpi;
import org.keycloak.connections.jpa.updater.JpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.JpaUpdaterSpi;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionSpi;
import org.keycloak.connections.jpa.updater.liquibase.lock.LiquibaseDBLockProviderFactory;
import org.keycloak.events.jpa.JpaEventStoreProviderFactory;
import org.keycloak.migration.MigrationProviderFactory;
import org.keycloak.migration.MigrationSpi;
import org.keycloak.models.IdentityProviderStorageSpi;
import org.keycloak.models.dblock.DBLockSpi;
import org.keycloak.models.jpa.JpaClientProviderFactory;
import org.keycloak.models.jpa.JpaClientScopeProviderFactory;
import org.keycloak.models.jpa.JpaGroupProviderFactory;
import org.keycloak.models.jpa.JpaIdentityProviderStorageProviderFactory;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.models.jpa.JpaRoleProviderFactory;
import org.keycloak.models.jpa.JpaUserProviderFactory;
import org.keycloak.models.jpa.session.JpaRevokedTokensPersisterProviderFactory;
import org.keycloak.models.jpa.session.JpaUserSessionPersisterProviderFactory;
import org.keycloak.models.session.RevokedTokenPersisterSpi;
import org.keycloak.models.session.UserSessionPersisterSpi;
import org.keycloak.organization.OrganizationSpi;
import org.keycloak.organization.jpa.JpaOrganizationProviderFactory;
import org.keycloak.protocol.LoginProtocolFactory;
import org.keycloak.protocol.LoginProtocolSpi;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.storage.DatastoreSpi;
import org.keycloak.storage.datastore.DefaultDatastoreProviderFactory;
import org.keycloak.testsuite.model.Config;
import org.keycloak.testsuite.model.KeycloakModelParameters;

import com.google.common.collect.ImmutableSet;

/**
 *
 * @author hmlnarik
 */
public class Jpa extends KeycloakModelParameters {

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      // jpa-specific
      .add(JpaConnectionSpi.class)
      .add(JpaUpdaterSpi.class)
      .add(LiquibaseConnectionSpi.class)
      .add(UserSessionPersisterSpi.class)
      .add(RevokedTokenPersisterSpi.class)

      .add(DatastoreSpi.class)

      //required for migrateModel
      .add(MigrationSpi.class)
      .add(LoginProtocolSpi.class)

      .add(DBLockSpi.class)

      //required for FederatedIdentityModel
      .add(IdentityProviderStorageSpi.class)
      .add(IdentityProviderSpi.class)

      .add(OrganizationSpi.class)

      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      // jpa-specific
      .add(DefaultDatastoreProviderFactory.class)

      .add(DefaultJpaConnectionProviderFactory.class)
      .add(JPAAuthorizationStoreFactory.class)
      .add(JpaClientProviderFactory.class)
      .add(JpaClientScopeProviderFactory.class)
      .add(JpaEventStoreProviderFactory.class)
      .add(JpaGroupProviderFactory.class)
      .add(JpaIdentityProviderStorageProviderFactory.class)
      .add(JpaRealmProviderFactory.class)
      .add(JpaRoleProviderFactory.class)
      .add(JpaUpdaterProviderFactory.class)
      .add(JpaUserProviderFactory.class)
      .add(LiquibaseConnectionProviderFactory.class)
      .add(LiquibaseDBLockProviderFactory.class)
      .add(JpaUserSessionPersisterProviderFactory.class)
      .add(JpaRevokedTokensPersisterProviderFactory.class)

      //required for migrateModel
      .add(MigrationProviderFactory.class)
      .add(LoginProtocolFactory.class)

      //required for FederatedIdentityModel
      .add(IdentityProviderFactory.class)

      .add(JpaOrganizationProviderFactory.class)

      .build();

    public Jpa() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }


    @Override
    public void updateConfig(Config cf) {
        updateConfigForJpa(cf);
    }

    public static void updateConfigForJpa(Config cf) {
        cf.spi("client").defaultProvider("jpa")
          .spi("clientScope").defaultProvider("jpa")
          .spi("group").defaultProvider("jpa")
          .spi("idp").defaultProvider("jpa")
          .spi("role").defaultProvider("jpa")
          .spi("user").defaultProvider("jpa")
          .spi("realm").defaultProvider("jpa")
          .spi("deploymentState").defaultProvider("jpa")
          .spi("dblock").defaultProvider("jpa")
        ;
// Use this for running model tests with Postgres database
//        cf.spi("connectionsJpa")
//                .provider("default")
//                .config("url", "jdbc:postgresql://localhost:5432/keycloakDB")
//                .config("user", "keycloak")
//                .config("password", "pass")
//                .config("driver", "org.postgresql.Driver");
//
    }
}
