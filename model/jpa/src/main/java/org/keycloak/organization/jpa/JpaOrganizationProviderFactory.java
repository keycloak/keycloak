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

package org.keycloak.organization.jpa;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmModel.RealmRemovedEvent;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.OrganizationProviderFactory;
import org.keycloak.provider.ProviderEvent;

public class JpaOrganizationProviderFactory implements OrganizationProviderFactory {

    @Override
    public OrganizationProvider create(KeycloakSession session) {
        return new JpaOrganizationProvider(session);
    }

    @Override
    public void init(Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        factory.register(this::handleRealmRemovedEvent);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "jpa";
    }

    private void handleRealmRemovedEvent(ProviderEvent event) {
        if (event instanceof RealmRemovedEvent) {
            KeycloakSession session = ((RealmRemovedEvent) event).getKeycloakSession();
            OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
            RealmModel realm = ((RealmRemovedEvent) event).getRealm();
            provider.removeOrganizations(realm);
        }
    }
}
