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

import jakarta.persistence.EntityManagerFactory;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import org.jboss.logging.Logger;

public final class NamedJpaConnectionProviderFactory extends AbstractJpaConnectionProviderFactory {

    private static final Logger logger = Logger.getLogger(NamedJpaConnectionProviderFactory.class);

    private String unitName;
    private String dataSourceName;

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Skip an inactive datasource's persistence unit instead of failing to resolve its (deactivated) EntityManagerFactory.
        String dsName = dataSourceName != null ? dataSourceName : unitName;
        var dsInstance = Arc.requireContainer().select(AgroalDataSource.class, new DataSource.DataSourceLiteral(dsName));
        if (dsInstance.isResolvable() && !dsInstance.getHandle().getBean().isActive()) {
            if (!isExplicitlyDisabled(dsName)) {
                logger.warnf("Datasource '%s' is not active, so the '%s' persistence unit is skipped."
                        + " If it should be active, configure it using datasource options like 'db-kind-%s'.", dsName, unitName, dsName);
            }
            return;
        }
        super.postInit(factory);
    }

    private static boolean isExplicitlyDisabled(String dsName) {
        return "false".equals(Configuration.getConfigValue("quarkus.datasource.\"" + dsName + "\".active").getValue());
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

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    @Override
    public String getId() {
        return unitName;
    }
}
