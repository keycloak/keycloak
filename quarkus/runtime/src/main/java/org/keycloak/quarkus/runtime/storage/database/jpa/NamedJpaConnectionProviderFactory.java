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

import java.util.function.Supplier;
import javax.persistence.EntityManagerFactory;
import org.keycloak.connections.jpa.DefaultJpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

public final class NamedJpaConnectionProviderFactory extends AbstractJpaConnectionProviderFactory {

    private String unitName;

    @Override
    public JpaConnectionProvider create(KeycloakSession session) {
        return new DefaultJpaConnectionProvider(createEntityManager(entityManagerFactory, session));
    }

    @Override
    protected EntityManagerFactory getEntityManagerFactory() {
        return getEntityManagerFactory(unitName).orElseThrow(new Supplier<IllegalStateException>() {
            @Override
            public IllegalStateException get() {
                return new IllegalStateException("Could not resolve named EntityManagerFactory [" + unitName + "]");
            }
        });
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    @Override
    public String getId() {
        return unitName;
    }
}
