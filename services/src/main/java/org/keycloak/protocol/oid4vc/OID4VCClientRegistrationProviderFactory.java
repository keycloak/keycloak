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

package org.keycloak.protocol.oid4vc;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.ClientRegistrationProviderFactory;

import java.util.List;

/**
 * Implementation of the {@link ClientRegistrationProviderFactory} to integrate the OID4VC protocols with
 * Keycloak's client-registration.
 * <p>
 * {@see https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class OID4VCClientRegistrationProviderFactory implements ClientRegistrationProviderFactory, OID4VCEnvironmentProviderFactory {

    @Override
    public ClientRegistrationProvider create(KeycloakSession session) {
        return new OID4VCClientRegistrationProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // no config required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do post init
    }

    @Override
    public void close() {
        // no resources to close
    }

    @Override
    public String getId() {
        return OID4VCLoginProtocolFactory.PROTOCOL_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return List.of();
    }
}