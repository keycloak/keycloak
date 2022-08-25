/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import javax.enterprise.inject.Instance;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.SynchronizationType;
import org.hibernate.internal.SessionFactoryImpl;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.connections.jpa.PersistenceExceptionConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;

public abstract class AbstractJpaConnectionProviderFactory implements JpaConnectionProviderFactory {

    protected Config.Scope config;
    protected Boolean xaEnabled;
    protected EntityManagerFactory entityManagerFactory;

    @Override
    public Connection getConnection() {
        SessionFactoryImpl entityManagerFactory = this.entityManagerFactory.unwrap(SessionFactoryImpl.class);

        try {
            return entityManagerFactory.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to obtain JDBC connection", cause);
        }
    }

    @Override
    public String getSchema() {
        return Configuration.getRawValue("kc.db-schema");
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
        xaEnabled = "xa".equals(Configuration.getRawValue("kc.transaction-xa-enabled"));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        entityManagerFactory = getEntityManagerFactory();
    }

    @Override
    public void close() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    protected abstract EntityManagerFactory getEntityManagerFactory();

    protected Optional<EntityManagerFactory> getEntityManagerFactory(String unitName) {
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

    protected EntityManager createEntityManager(EntityManagerFactory emf, KeycloakSession session) {
        EntityManager entityManager;

        if (xaEnabled) {
            entityManager = PersistenceExceptionConverter.create(session, emf.createEntityManager(SynchronizationType.SYNCHRONIZED));
        } else {
            entityManager = PersistenceExceptionConverter.create(session, emf.createEntityManager());
        }

        entityManager.setFlushMode(FlushModeType.AUTO);

        return entityManager;
    }
}
