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

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.provider.ProviderConfigProperty;

public class SecureClientUrisPatternExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-client-uris-pattern";

    public static final String ALLOWED_PATTERNS = "allowed-patterns";
    public static final String CLIENT_URI_FIELDS = "client-uri-fields";

    public static final List<String> ALL_CLIENT_URI_FIELDS = Arrays.asList(
            "rootUrl",
            "adminUrl",
            "baseUrl",
            "redirectUris",
            "webOrigins",

            //attributes
            "jwksUri",
            "requestUris",
            "backchannelLogoutUrl",
            "postLogoutRedirectUris",
            "cibaClientNotificationEndpoint",
            OIDCConfigAttributes.LOGO_URI,
            OIDCConfigAttributes.POLICY_URI,
            OIDCConfigAttributes.TOS_URI,
            OIDCConfigAttributes.SECTOR_IDENTIFIER_URI
    );

    private static final ProviderConfigProperty CLIENT_URI_FIELDS_PROPERTY = new ProviderConfigProperty(
            CLIENT_URI_FIELDS,
            "Client URI Fields to validate",
            "Select the specific client URI fields to validate. If no fields are selected, ALL known URI fields listed above will be validated by default.",
            ProviderConfigProperty.MULTIVALUED_LIST_TYPE,
            null,
            ALL_CLIENT_URI_FIELDS.toArray(new String[0]));

    private static final ProviderConfigProperty ALLOWED_PATTERNS_PROPERTY = new ProviderConfigProperty(
            ALLOWED_PATTERNS,
            "Allowed URI Patterns",
            "A client URI is considered valid ONLY if it matches at least one of these regex patterns. If no valid patterns are configured, validation will fail for any URI.",
            ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
            null);

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureClientUrisPatternExecutor(session);
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
        return "Enforces security policies on client URIs by validating them against regex patterns. If a URI does not match any configured pattern, the request is rejected.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(ALLOWED_PATTERNS_PROPERTY, CLIENT_URI_FIELDS_PROPERTY);
    }

}
