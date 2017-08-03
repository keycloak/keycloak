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
package org.keycloak.social.gitlab;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

/**
 * @author Pedro Igor
 */
public class GitLabIdentityProviderFactory extends AbstractIdentityProviderFactory<GitLabIdentityProvider> implements SocialIdentityProviderFactory<GitLabIdentityProvider> {

    public static final String PROVIDER_ID = "gitlab";

    @Override
    public String getName() {
        return "GitLab";
    }

    @Override
    public GitLabIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new GitLabIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
