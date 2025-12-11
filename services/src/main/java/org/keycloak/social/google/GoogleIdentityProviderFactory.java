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

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
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
        return new GoogleIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // The supported authentication URI parameters can be found in the google identity documentation
        // See: https://developers.google.com/identity/openid-connect/openid-connect#authenticationuriparameters
        return ProviderConfigurationBuilder.create()
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
    }
}
