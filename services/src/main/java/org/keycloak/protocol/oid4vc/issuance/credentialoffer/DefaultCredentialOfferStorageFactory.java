/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.issuance.credentialoffer;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for {@link DefaultCredentialOfferStorage}.
 * 
 * <p>This factory provides the default cluster-aware implementation of credential offer storage.
 * The storage uses Keycloak's distributed cache infrastructure, making it suitable for
 * clustered and cross-DC deployments.
 */
public class DefaultCredentialOfferStorageFactory implements CredentialOfferStorageFactory {

    private static CredentialOfferStorage INSTANCE;

    @Override
    public CredentialOfferStorage create(KeycloakSession session) {
        if (INSTANCE == null) {
            INSTANCE = new DefaultCredentialOfferStorage();
        }
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return "default";
    }
}
