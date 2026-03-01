/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.executor;

import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * <p>Factory that allows to configure different audiences as valid for the JWT Authorization Grant.
 * This behavior breaks the specification and can have security implications.</p>
 *
 * @author rmartinc
 */
public class JWTAuthorizationGrantAudienceExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "jwt-authorization-grant-audience";
    public static final String ALLOWED_AUDIENCE = "allowed-audience";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new JWTAuthorizationGrantAudienceExecutor(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return """
               Executor that configures new and exclusive valid audiences for the JWT Authorization Grant type.
               The default audiences valid for the grant are not valid anymore.
               Note this behavior breaks the standard and can have major security implications.
               """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(ALLOWED_AUDIENCE)
                .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
                .label("Allowed audience")
                .helpText("List of new and exclusive valid audiences for the JWT Authhorization Grant")
                .add()
                .build();
    }
}
