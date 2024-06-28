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
 *
 */

package org.keycloak.testsuite.wellknown;

import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CustomOIDCWellKnownProvider extends OIDCWellKnownProvider {

    public CustomOIDCWellKnownProvider(KeycloakSession session, Map<String, Object> openidConfigOverride, boolean includeClientScopes) {
        super(session, openidConfigOverride, includeClientScopes);
    }

    @Override
    public Object getConfig() {
        OIDCConfigurationRepresentation config = (OIDCConfigurationRepresentation) super.getConfig();
        config.getOtherClaims().put("foo", "bar");
        return config;
    }

    @Override
    protected MTLSEndpointAliases getMtlsEndpointAliases(OIDCConfigurationRepresentation config) {
        MTLSEndpointAliases mtlsEndpointAliases = super.getMtlsEndpointAliases(config);
        mtlsEndpointAliases.setRegistrationEndpoint("https://placeholder-host-set-by-testsuite-provider/registration");
        return mtlsEndpointAliases;
    }
}
