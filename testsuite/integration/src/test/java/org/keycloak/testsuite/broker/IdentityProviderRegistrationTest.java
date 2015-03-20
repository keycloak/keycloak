/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.broker;

import org.junit.Test;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.social.SocialIdentityProvider;
import org.keycloak.social.SocialIdentityProviderFactory;
import org.keycloak.testsuite.broker.provider.CustomIdentityProvider;
import org.keycloak.testsuite.broker.provider.social.CustomSocialProvider;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public class IdentityProviderRegistrationTest extends AbstractIdentityProviderModelTest {

    @Test
    public void testIdentityProviderRegistration() {
        Set<String> installedProviders = getInstalledProviders();

        for (String providerId : getExpectedProviders()) {
            if (!installedProviders.contains(providerId)) {
                fail("Provider [" + providerId + "] not installed ");
            }
        }
    }

    @Test
    public void testCustomSocialProviderRegistration() {
        String providerId = "custom-social-provider";

        assertTrue(getInstalledProviders().contains(providerId));

        SocialIdentityProviderFactory<CustomSocialProvider> providerFactory = (SocialIdentityProviderFactory) this.session.getKeycloakSessionFactory().getProviderFactory(SocialIdentityProvider.class, providerId);

        assertNotNull(providerFactory);

        IdentityProviderModel identityProviderModel = new IdentityProviderModel();

        identityProviderModel.setAlias("custom-provider");

        CustomSocialProvider customSocialProvider = providerFactory.create(identityProviderModel);

        assertNotNull(customSocialProvider);
        IdentityProviderModel config = customSocialProvider.getConfig();

        assertNotNull(config);
        assertEquals("custom-provider", config.getAlias());
    }

    @Test
    public void testCustomIdentityProviderRegistration() {
        String providerId = "custom-identity-provider";

        assertTrue(getInstalledProviders().contains(providerId));

        IdentityProviderFactory<CustomIdentityProvider> providerFactory = (IdentityProviderFactory) this.session.getKeycloakSessionFactory().getProviderFactory(IdentityProvider.class, providerId);

        assertNotNull(providerFactory);

        IdentityProviderModel identityProviderModel = new IdentityProviderModel();

        identityProviderModel.setAlias("custom-provider");

        CustomIdentityProvider provider = providerFactory.create(identityProviderModel);

        assertNotNull(provider);
        IdentityProviderModel config = provider.getConfig();

        assertNotNull(config);
        assertEquals("custom-provider", config.getAlias());
    }

    private Set<String> getInstalledProviders() {
        Set<String> installedProviders = this.session.listProviderIds(IdentityProvider.class);

        installedProviders.addAll(this.session.listProviderIds(SocialIdentityProvider.class));

        return installedProviders;
    }
}
