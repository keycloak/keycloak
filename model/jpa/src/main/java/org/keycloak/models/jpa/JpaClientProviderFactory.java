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
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.keycloak.Config;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.entities.RealmAttributes;
import org.keycloak.protocol.saml.SamlConfigAttributes;

import static org.keycloak.models.jpa.JpaRealmProviderFactory.PROVIDER_ID;
import static org.keycloak.models.jpa.JpaRealmProviderFactory.PROVIDER_PRIORITY;

public class JpaClientProviderFactory implements ClientProviderFactory {

    private Set<String> clientSearchableAttributes = null;

    private static final List<String> REQUIRED_SEARCHABLE_ATTRIBUTES = Arrays.asList(
        "saml_idp_initiated_sso_url_name",
        SamlConfigAttributes.SAML_ARTIFACT_BINDING_IDENTIFIER,
        "jwt.credential.issuer",
        "jwt.credential.sub"
    );

    @Override
    public void init(Config.Scope config) {
        String[] searchableAttrsArr = config.getArray("searchableAttributes");
        if (searchableAttrsArr == null) {
            String s = System.getProperty("keycloak.client.searchableAttributes");
            searchableAttrsArr = s == null ? null : s.split("\\s*,\\s*");
        }
        HashSet<String> s = new HashSet<>(REQUIRED_SEARCHABLE_ATTRIBUTES);
        if (searchableAttrsArr != null) {
            s.addAll(Arrays.asList(searchableAttrsArr));
        }
        clientSearchableAttributes = Collections.unmodifiableSet(s);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        if (Profile.isFeatureEnabled(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2)) {
            factory.register(event -> {
                if (event instanceof RealmModel.RealmAttributeUpdateEvent attrUpdateEvent) {
                    if (Objects.equals(attrUpdateEvent.getAttributeName(), RealmAttributes.ADMIN_PERMISSIONS_ENABLED) && Boolean.parseBoolean(attrUpdateEvent.getAttributeValue())) {
                        KeycloakSession keycloakSession = attrUpdateEvent.getKeycloakSession();
                        RealmModel realm = attrUpdateEvent.getRealm();
                        AdminPermissionsSchema.SCHEMA.init(keycloakSession, realm);
                    }
                }
            });
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ClientProvider create(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new JpaRealmProvider(session, em, clientSearchableAttributes, null);
    }

    @Override
    public void close() {
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }

}
