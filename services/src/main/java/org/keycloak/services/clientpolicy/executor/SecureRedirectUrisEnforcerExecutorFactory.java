/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

public class SecureRedirectUrisEnforcerExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-redirect-uris-enforcer";

    public static final String ALLOW_IPV4_LOOPBACK_ADDRESS = "allow-ipv4-loopback-address";
    public static final String ALLOW_IPV6_LOOPBACK_ADDRESS = "allow-ipv6-loopback-address";
    public static final String ALLOW_PRIVATE_USE_URI_SCHEME = "allow-private-use-uri-scheme";

    public static final String ALLOW_HTTP_SCHEME = "allow-http-scheme";
    public static final String ALLOW_WILDCARD_CONTEXT_PATH = "allow-wildcard-context-path";
    public static final String ALLOW_PERMITTED_DOMAINS = "allow-permitted-domains";
    public static final String OAUTH_2_1_COMPLIANT = "oauth-2-1-compliant";

    public static final String ALLOW_OPEN_REDIRECT = "allow-open-redirect";

    public enum UriType {
        NORMAL_URI,
        IPV4_LOOPBACK_ADDRESS,
        IPV6_LOOPBACK_ADDRESS,
        PRIVATE_USE_URI_SCHEME,
        INVALID_URI
    }

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureRedirectUrisEnforcerExecutor(session);
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
        return "On registering and updating a client, this executor only allows a valid redirect uri. On receiving an authorization request, this executor checks whether a redirect uri parameter matches registered redirect uris in the way that depends on the executor's setting.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    static {
        CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()

            .property()
            .name(ALLOW_IPV4_LOOPBACK_ADDRESS)
            .label("Allow IPv4 loopback address")
            .helpText("If ON, then the executor allows IPv4 loopback address as a valid redirect uri. " +
                "For example, 'http://127.0.0.1:{port}/{path}' . ")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_IPV6_LOOPBACK_ADDRESS)
            .label("Allow IPv6 loopback address")
            .helpText("If ON, then the executor allows IPv6 loopback address as a valid redirect uri. " +
                "For example, 'http://[::1]:{port}/{path}' . ")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_PRIVATE_USE_URI_SCHEME)
            .label("Allow private use URI scheme")
            .helpText("If ON, then the executor allows a private-use URI scheme (aka custom URL scheme) as a valid redirect uri. " +
                "For example, an app that controls the domain name 'app.example.com' " +
                "can use 'com.example.app' as their scheme.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_HTTP_SCHEME)
            .label("Allow http scheme")
            .helpText("If On, then the executor allows http scheme as a valid redirect uri.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_WILDCARD_CONTEXT_PATH)
            .label("Allow wildcard in context-path")
            .helpText("If ON, then it will allow wildcard in context-path uris. " +
                "For example, domain.example.com/*")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_PERMITTED_DOMAINS)
            .label("Allow permitted domains")
            .helpText("If some domains are filled, the redirect uri host must match one of registered domains. " +
                "If not filled, then all domains are possible to use. The domains are checked by using regex. " +
                "For example use pattern like this '(.*)\\.example\\.org' if you want clients to register redirect-uris only from domain 'example.org'." +
                "Don't forget to use escaping of special characters like dots as otherwise dot is interpreted as any character in regex!")
            .type(ProviderConfigProperty.MULTIVALUED_STRING_TYPE)
            .add()

            .property()
            .name(OAUTH_2_1_COMPLIANT)
            .label("OAuth 2.1 Compliant")
            .helpText("If On, then the executor checks and matches the uri by following OAuth 2.1 specification. This means that for example URL fragments, wildcard redirect uris or URL using 'localhost' are not allowed.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .property()
            .name(ALLOW_OPEN_REDIRECT)
            .label("Allow open redirect")
            .helpText("If ON, then the executor does not verify a redirect uri even if its other setting is ON. " +
                "WARNING: This is insecure and should be used with care as open redirects are bad practice.")
            .type(ProviderConfigProperty.BOOLEAN_TYPE)
            .defaultValue(false)
            .add()

            .build();
    }
}
