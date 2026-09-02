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
package org.keycloak.social.google;

import java.util.List;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author Pedro Igor
 */
public class GoogleIdentityProviderFactory extends AbstractIdentityProviderFactory<GoogleIdentityProvider> implements SocialIdentityProviderFactory<GoogleIdentityProvider> {

    public static final String PROVIDER_ID = "google";

    @Override
    public String getName() {
        return "Google";
    }

    @Override
    public GoogleIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new GoogleIdentityProvider(session, new GoogleIdentityProviderConfig(model));
    }

    @Override
    public GoogleIdentityProviderConfig createConfig() {
        GoogleIdentityProviderConfig config = new  GoogleIdentityProviderConfig();
        config.getConfig().put(IdentityProviderModel.ISSUER, GoogleIdentityProvider.ISSUER_URL);
        return config;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // The supported authentication URI parameters can be found in the google identity documentation
        // See: https://developers.google.com/identity/openid-connect/openid-connect#authenticationuriparameters
        List<ProviderConfigProperty> providerConfigProperties = ProviderConfigurationBuilder.create()
                .property().name("prompt")
                .label("Prompt")
                .helpText("Set 'prompt' query parameter when logging in with Google. The allowed values are 'none', 'consent' and 'select_account'. " +
                        "If no value is specified and the user has not previously authorized access, then the user is shown a consent screen.")
                .type(ProviderConfigProperty.STRING_TYPE).add()
                .property().name("hostedDomain")
                .label("Hosted Domain")
                .helpText("Set 'hd' query parameter when logging in with Google. Google will list accounts only for this " +
                        "domain. Keycloak validates that the returned identity token has a claim for this domain. When '*' " +
                        "is entered, any hosted account can be used. Comma ',' separated list of domains is supported.")
                .type(ProviderConfigProperty.STRING_TYPE).add()
                .property().name("userIp")
                .label("Use userIp param")
                .helpText("Set 'userIp' query parameter when invoking on Google's User Info service.  This will use the " +
                        "user's ip address.  Useful if Google is throttling access to the User Info service.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE).add()
                .property().name("offlineAccess")
                .label("Request refresh token")
                .helpText("Set 'access_type' query parameter to 'offline' when redirecting to google authorization " +
                        "endpoint, to get a refresh token back. Useful if planning to use Token Exchange to retrieve " +
                        "Google token to access Google APIs when the user is not at the browser.")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .add().build();

        if (Profile.isFeatureEnabled(Profile.Feature.JWT_AUTHORIZATION_GRANT)) {
            //easier to add to previous builder when feature will be supported
            providerConfigProperties.addAll(ProviderConfigurationBuilder.create()
                    .property().name(JWTAuthorizationGrantConfig.JWT_AUTHORIZATION_GRANT_ENABLED)
                    .label("JWT Authorization Grant")
                    .helpText("Enable the Google identity provider to act as a trust provider to validate " +
                            "authorization grant JWT assertions (Google ID Token) according to RFC 7523, " +
                            "except for the audience claim that must contain the client id of the configured client")
                    .type(ProviderConfigProperty.BOOLEAN_TYPE).add().build());

            providerConfigProperties.addAll(ProviderConfigurationBuilder.create()
                    .property().name(JWTAuthorizationGrantConfig.JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION)
                    .label("Max allowed assertion expiration")
                            .defaultValue("3600")
                    .helpText("This property is used only for JWT Authorization Grant" +
                            " to set the max accepted duration limit for the assertion. " +
                            "Note that the Google ID Token expires after 1 hour, so this property can be used to limit the time during which the assertion can be used.")
                    .type(ProviderConfigProperty.NUMBER_TYPE).add().build());
        }

        return providerConfigProperties;
    }
}
