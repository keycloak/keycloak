/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan.organization;

import org.keycloak.Config.Scope;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.models.OrganizationModel;
import org.keycloak.organization.OrganizationProviderFactory;

public class InfinispanOrganizationProviderFactory implements OrganizationProviderFactory {

    public static final String PROVIDER_ID = "infinispan";

    @Override
    public OrganizationProvider create(KeycloakSession session) {
        return new InfinispanOrganizationProvider(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(event -> {
            if (event instanceof RealmModel.IdentityProviderUpdatedEvent idpUpdatedEvent) {
                registerOrganizationInvalidation(idpUpdatedEvent.getKeycloakSession(), idpUpdatedEvent.getUpdatedIdentityProvider());
            }
            if (event instanceof RealmModel.IdentityProviderRemovedEvent idpRemovedEvent) {
                registerOrganizationInvalidation(idpRemovedEvent.getKeycloakSession(), idpRemovedEvent.getRemovedIdentityProvider());
            }
        });
    }

    private void registerOrganizationInvalidation(KeycloakSession session, IdentityProviderModel idp) {
        if (idp.getConfig().get(OrganizationModel.ORGANIZATION_ATTRIBUTE) != null) {
            InfinispanOrganizationProvider orgProvider = (InfinispanOrganizationProvider) session.getProvider(OrganizationProvider.class, getId());
            if (orgProvider != null) {
                orgProvider.registerOrganizationInvalidation(idp.getConfig().get(OrganizationModel.ORGANIZATION_ATTRIBUTE));
            }
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public int order() {
        return 10;
    }
}
