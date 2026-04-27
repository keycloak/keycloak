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
package org.keycloak.social.github;

import java.util.List;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author Pedro Igor
 */
public class GitHubIdentityProviderFactory extends AbstractIdentityProviderFactory<GitHubIdentityProvider> implements SocialIdentityProviderFactory<GitHubIdentityProvider> {

    public static final String PROVIDER_ID = "github";

    @Override
    public String getName() {
        return "GitHub";
    }

    @Override
    public GitHubIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new GitHubIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
    }

    @Override
    public OAuth2IdentityProviderConfig createConfig() {
        return new OAuth2IdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create().property()
                .name(GitHubIdentityProvider.BASE_URL_KEY).label("Base URL").helpText("Override the default Base URL for this identity provider.")
                .type(ProviderConfigProperty.STRING_TYPE).add().property()
                .name(GitHubIdentityProvider.API_URL_KEY).label("API URL").helpText("Override the default API URL for this identity provider.")
                .type(ProviderConfigProperty.STRING_TYPE).add().property()
                .name(GitHubIdentityProvider.GITHUB_JSON_FORMAT_KEY).label("JSON Format").helpText("Enable to receive JSON format responses from GitHub. This is also required to automatically refresh access tokens retrieved from GitHub.")
                .defaultValue(false).type(ProviderConfigProperty.BOOLEAN_TYPE).add().build();
    }
}
