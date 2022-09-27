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

package org.keycloak.quarkus.runtime.storage.database.jpa;

import static org.keycloak.config.StorageOptions.STORAGE;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalValue;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.enterprise.inject.Instance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.keycloak.config.StorageOptions;
import org.keycloak.models.ModelException;
import org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;

public class QuarkusJpaMapStorageProviderFactory extends JpaMapStorageProviderFactory {

    @Override
    public String getId() {
        return StorageOptions.StorageType.jpa.getProvider();
    }

    @Override
    protected EntityManagerFactory createEntityManagerFactory() {
        Instance<EntityManagerFactory> instance = Arc.container().select(EntityManagerFactory.class);

        if (instance.isResolvable()) {
            return instance.get();
        }

        return getEntityManagerFactory("keycloak-default").orElseThrow(() -> new IllegalStateException("Failed to resolve the default entity manager factory"));
    }

    @Override
    protected EntityManager getEntityManager() {
        EntityManager em = super.getEntityManager();
        try {
            Connection connection = em.unwrap(SessionImpl.class).connection();
            // In the Undertow setup, Hibernate sets the connection to non-autocommit, and in the Quarkus setup the XA transaction manager does this.
            // For the Quarkus setup without a XA transaction manager, we didn't find a way to have this setup automatically.
            // There is also no known option to configure this in the Agroal DB connection pool in a Quarkus setup:
            // While the connection pool supports it, it hasn't been exposed as a Quarkus configuration option.
            // At the same time, disabling autocommit is essential to keep the transactional boundaries of the application.
            // The failure we've seen is the failed unique constraints that are usually deferred (for example, for client attributes).
            // A follow-up issue to track this is here: https://github.com/keycloak/keycloak/issues/13222
            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }
        } catch (SQLException e) {
            throw new ModelException("unable to set non-auto-commit to false");
        }
        return em;
    }

    @Override
    protected Connection getConnection() {
        SessionFactoryImpl entityManagerFactory = getEntityManagerFactory().unwrap(SessionFactoryImpl.class);
        try {
            return entityManagerFactory.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to obtain JDBC connection", cause);
        }
    }

    private Optional<EntityManagerFactory> getEntityManagerFactory(String unitName) {
        Instance<EntityManagerFactory> instance = Arc.container().select(EntityManagerFactory.class, new PersistenceUnit() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return PersistenceUnit.class;
            }

            @Override
            public String value() {
                return unitName;
            }
        });

        if (instance.isResolvable()) {
            return Optional.of(instance.get());
        }

        return Optional.empty();
    }

}
