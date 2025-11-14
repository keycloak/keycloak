/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.jpa.session;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.session.UserSessionPersisterProviderFactory;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JpaUserSessionPersisterProviderFactory implements UserSessionPersisterProviderFactory, ServerInfoAwareProviderFactory {

    public static final String ID = "jpa";

    private static final String EXPIRATION_BATCH_CONFIG = "expirationBatch";
    // Using 512 as default. From Gemini:
    // * Oracle: Has a strict limit of 1000 expressions in a single IN list (ORA-01795).
    // * SQL Server: Has limits based on the maximum number of parameters in an RPC, often hitting a limit of 2100 parameters.
    // * PostgreSQL/MySQL: Generally have much higher limits or no practical limit.
    public static final int DEFAULT_EXPIRATION_BATCH = 512;

    private int expirationBatch = DEFAULT_EXPIRATION_BATCH;

    @Override
    public UserSessionPersisterProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JpaUserSessionPersisterProvider(session, em, expirationBatch);
    }

    @Override
    public void init(Config.Scope config) {
        // We do not set a maximum batch size, because Hibernate should be able to handle it.
        // From Gemini:
        // Modern versions of Hibernate (especially in their database Dialects for Oracle) are often aware of the 1000-item
        // limitation and automatically implement a workaround when you pass a collection larger than the limit.
        // Hibernate will automatically generate SQL that splits the large list into multiple IN clauses connected by OR:
        // "WHERE id IN (1, 2, 3, ..., 1000) OR id IN (1001, 1002, ...)"
        expirationBatch = Math.max(1, config.getInt(EXPIRATION_BATCH_CONFIG, DEFAULT_EXPIRATION_BATCH));
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public Set<Class<? extends Provider>> dependsOn() {
        return Set.of(JpaConnectionProvider.class);
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        var builder = ProviderConfigurationBuilder.create();
        builder.property()
                .name(EXPIRATION_BATCH_CONFIG)
                .helpText("Sets the size of the expiration batch, i.e., the number of expired sessions to remove per delete statement.")
                .label("size")
                .type(ProviderConfigProperty.INTEGER_TYPE)
                .defaultValue(DEFAULT_EXPIRATION_BATCH)
                .add();
        return builder.build();
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return Map.of(EXPIRATION_BATCH_CONFIG, Integer.toString(expirationBatch));
    }
}
