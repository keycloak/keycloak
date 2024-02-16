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
package org.keycloak.social.linkedin;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Deprecated
public class LinkedInIdentityProviderFactory extends AbstractIdentityProviderFactory<LinkedInIdentityProvider>
        implements SocialIdentityProviderFactory<LinkedInIdentityProvider>, EnvironmentDependentProviderFactory  {

    public static final String PROVIDER_ID = "linkedin";

    @Override
    public String getName() {
        return "LinkedIn (deprecated)";
    }

    @Override
    public LinkedInIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new LinkedInIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
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
        return ProviderConfigurationBuilder.create()
                .property().name("profileProjection")
                .label("Profile projection")
                .helpText("Projection parameter for profile request. Leave empty for default projection.")
                .add().build();
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.LINKEDIN_OAUTH);
    }
}
