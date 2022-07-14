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
import javax.persistence.EntityManagerFactory;
import org.hibernate.internal.SessionFactoryImpl;
import org.keycloak.config.StorageOptions;
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
    protected Connection getConnection() {
        SessionFactoryImpl entityManagerFactory = getEntityManagerFactory().unwrap(SessionFactoryImpl.class);

        try {
            return entityManagerFactory.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection();
        } catch (SQLException cause) {
            throw new RuntimeException("Failed to obtain JDBC connection", cause);
        }
    }

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

    @Override
    public boolean isSupported() {
        // allow running multiple storages at runtime if jpa is enabled, otherwise disables it
        // only enable jpa store if explicitly said so
        // this is because jpa store is missing some provider implementations that require the usage of another storage at runtime like chm
        // once we have jpa-based provider impls for all areas we can remove this and be able to run jpa store as the single store
        return StorageOptions.StorageType.jpa.name().equals(getOptionalValue(NS_KEYCLOAK_PREFIX.concat(STORAGE.getKey())).orElse(null));
    }
}
