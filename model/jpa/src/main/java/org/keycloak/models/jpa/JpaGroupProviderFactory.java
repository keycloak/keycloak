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

package org.keycloak.models.jpa;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.GroupProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import static org.keycloak.models.jpa.JpaRealmProviderFactory.PROVIDER_ID;
import static org.keycloak.models.jpa.JpaRealmProviderFactory.PROVIDER_PRIORITY;

public class JpaGroupProviderFactory implements GroupProviderFactory {

    private Set<String> groupSearchableAttributes = null;
    private boolean escapeSlashesInGroupPath;

    @Override
    public void init(Config.Scope config) {
        escapeSlashesInGroupPath = config.getBoolean("escapeSlashesInGroupPath", GroupProvider.DEFAULT_ESCAPE_SLASHES);
        String[] searchableAttrsArr = config.getArray("searchableAttributes");
        if (searchableAttrsArr == null) {
            String s = System.getProperty("keycloak.group.searchableAttributes");
            searchableAttrsArr = s == null ? null : s.split("\\s*,\\s*");
        }
        if (searchableAttrsArr != null) {
            groupSearchableAttributes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(searchableAttrsArr)));
        }
        else {
            groupSearchableAttributes = Collections.emptySet();
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public GroupProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JpaRealmProvider(session, em, null, groupSearchableAttributes);
    }

    @Override
    public void close() {
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }

    @Override
    public boolean escapeSlashesInGroupPath() {
        return escapeSlashesInGroupPath;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("escapeSlashesInGroupPath")
                .helpText("If true slashes `/` in group names are escaped with the character `~` when converted to paths.")
                .type("boolean")
                .defaultValue(false)
                .add()
                .property()
                .name("searchableAttributes")
                .helpText("The list of attributes separated by comma that are allowed in client attribute searches.")
                .type("string")
                .add()
                .build();
    }
}
