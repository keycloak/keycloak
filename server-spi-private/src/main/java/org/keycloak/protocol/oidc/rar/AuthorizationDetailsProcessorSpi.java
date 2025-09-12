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
package org.keycloak.protocol.oidc.rar;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI for authorization details processors that handle the authorization_details parameter
 * in OAuth2/OIDC authorization and token requests as per RAR (Rich Authorization Requests) specification.
 * The authorization_details parameter can be used in both authorization requests and token requests
 * as specified in the OpenID for Verifiable Credential Issuance specification.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class AuthorizationDetailsProcessorSpi implements Spi {

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return "authorization-details-processor";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AuthorizationDetailsProcessor.class;
    }

    @Override
    public Class<? extends ProviderFactory<AuthorizationDetailsProcessor>> getProviderFactoryClass() {
        return AuthorizationDetailsProcessorFactory.class;
    }
}
