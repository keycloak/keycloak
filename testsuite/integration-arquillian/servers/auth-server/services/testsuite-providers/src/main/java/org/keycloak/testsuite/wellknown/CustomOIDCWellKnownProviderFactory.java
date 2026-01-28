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


import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.wellknown.WellKnownProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CustomOIDCWellKnownProviderFactory extends OIDCWellKnownProviderFactory {

    public static final String INCLUDE_CLIENT_SCOPES = "oidc.wellknown.include.client.scopes";

    @Override
    public WellKnownProvider create(KeycloakSession session) {
        return new CustomOIDCWellKnownProvider(session, getOpenidConfigOverride(), includeClientScopes());
    }

    private boolean includeClientScopes() {
        String includeClientScopesProp = System.getProperty("oidc.wellknown.include.client.scopes");
        return includeClientScopesProp == null || Boolean.parseBoolean(includeClientScopesProp);
    }

    @Override
    public void init(Config.Scope config) {
        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(CustomOIDCWellKnownProviderFactory.class.getClassLoader());
            initConfigOverrideFromFile("classpath:wellknown/oidc-well-known-config-override.json");
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    @Override
    public String getId() {
        return "custom-testsuite-oidc-well-known-factory";
    }

    @Override
    public String getAlias() {
        return OIDCWellKnownProviderFactory.PROVIDER_ID;
    }

    // Should be prioritized over default factory
    @Override
    public int getPriority() {
        return 1;
    }
}
