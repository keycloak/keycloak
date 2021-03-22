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

import org.keycloak.authorization.jpa.store.JPAAuthorizationStoreFactory;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.connections.jpa.JpaConnectionSpi;
import org.keycloak.connections.jpa.updater.JpaUpdaterProviderFactory;
import org.keycloak.connections.jpa.updater.JpaUpdaterSpi;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProviderFactory;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionSpi;
import org.keycloak.connections.jpa.updater.liquibase.lock.LiquibaseDBLockProviderFactory;
import org.keycloak.events.jpa.JpaEventStoreProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelParameters;
import org.keycloak.models.dblock.DBLockSpi;
import org.keycloak.models.jpa.JpaClientProviderFactory;
import org.keycloak.models.jpa.JpaClientScopeProviderFactory;
import org.keycloak.models.jpa.JpaGroupProviderFactory;
import org.keycloak.models.jpa.JpaRealmProviderFactory;
import org.keycloak.models.jpa.JpaRoleProviderFactory;
import org.keycloak.models.jpa.JpaUserProviderFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 *
 * @author hmlnarik
 */
public class Jpa extends KeycloakModelParameters {

    static final Set<Class<? extends Spi>> ALLOWED_SPIS = ImmutableSet.<Class<? extends Spi>>builder()
      // jpa-specific
      .add(DBLockSpi.class)
      .add(JpaConnectionSpi.class)
      .add(JpaUpdaterSpi.class)
      .add(LiquibaseConnectionSpi.class)

      .build();

    static final Set<Class<? extends ProviderFactory>> ALLOWED_FACTORIES = ImmutableSet.<Class<? extends ProviderFactory>>builder()
      // jpa-specific
      .add(DefaultJpaConnectionProviderFactory.class)
      .add(JPAAuthorizationStoreFactory.class)
      .add(JpaClientProviderFactory.class)
      .add(JpaClientScopeProviderFactory.class)
      .add(JpaEventStoreProviderFactory.class)
      .add(JpaGroupProviderFactory.class)
      .add(JpaRealmProviderFactory.class)
      .add(JpaRoleProviderFactory.class)
      .add(JpaUpdaterProviderFactory.class)
      .add(JpaUserProviderFactory.class)
      .add(LiquibaseConnectionProviderFactory.class)
      .add(LiquibaseDBLockProviderFactory.class)
      .build();

    public Jpa() {
        super(ALLOWED_SPIS, ALLOWED_FACTORIES);
    }

}
