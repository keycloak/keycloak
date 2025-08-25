/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.client;


import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Client authentication based on JWT signed by client private key .
 * See <a href="https://tools.ietf.org/html/rfc7519">specs</a> for more details.
 *
 * This is server side, which verifies JWT from client_assertion parameter, where the assertion was created on adapter side by
 * org.keycloak.adapters.authentication.JWTClientCredentialsProvider
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWTClientAuthenticator extends AbstractJWTClientAuthenticator {

    public static final String ATTR_PREFIX = AbstractJWTClientAuthenticator.ATTR_PREFIX;
    public static final String CERTIFICATE_ATTR = AbstractJWTClientAuthenticator.CERTIFICATE_ATTR;

    public static final String PROVIDER_ID = "client-jwt";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Signed Jwt";
    }

    @Override
    public String getHelpText() {
        return "Validates client based on signed JWT issued by client and signed with the Client private key";
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        Map<String, Object> props = new HashMap<>();
        props.put("client-keystore-file", "REPLACE WITH THE LOCATION OF YOUR KEYSTORE FILE");
        props.put("client-keystore-type", "jks");
        props.put("client-keystore-password", "REPLACE WITH THE KEYSTORE PASSWORD");
        props.put("client-key-password", "REPLACE WITH THE KEY PASSWORD IN KEYSTORE");
        props.put("client-key-alias", client.getClientId());
        props.put("token-timeout", 10);
        String algorithm = client.getAttribute(OIDCConfigAttributes.TOKEN_ENDPOINT_AUTH_SIGNING_ALG);
        if (algorithm != null) {
            props.put("algorithm", algorithm);
        }

        Map<String, Object> config = new HashMap<>();
        config.put("jwt", props);
        return config;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    protected JWTClientValidator getValidator(ClientAuthenticationFlowContext context) {
        return new JWTClientValidator(context, getId());
    }
}
