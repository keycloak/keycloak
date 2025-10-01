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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.organization.OrganizationInvitationProvider;
import org.keycloak.organization.OrganizationInvitationProviderFactory;

/**
 * JPA implementation of OrganizationInvitationProviderFactory.
 */
public class JpaOrganizationInvitationProviderFactory implements OrganizationInvitationProviderFactory {

    public static final String ID = "jpa";

    @Override
    public OrganizationInvitationProvider create(KeycloakSession session) {
        return new JpaOrganizationInvitationProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // No initialization required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization required
    }

    @Override
    public void close() {
        // No cleanup required
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return OrganizationInvitationProviderFactory.super.isSupported(config);
    }

    @Override
    public int order() {
        return 0;
    }
}
