/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.testsuite.user.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.testsuite.arquillian.annotation.SetDefaultProvider;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.userprofile.DeclarativeUserProfileProvider;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:joerg.matysiak@bosch.io">Jörg Matysiak</a>
 */
@SetDefaultProvider(spi="userProfile", providerId="custom-user-profile", defaultProvider="declarative-user-profile", onlyUpdateDefault = true)
public class CustomUserProfileTest extends AbstractUserProfileTest {
    
    @Test
    public void testCustomUserProfileProviderIsActive() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) CustomUserProfileTest::testCustomUserProfileProviderIsActive);
    }

    private static void testCustomUserProfileProviderIsActive(KeycloakSession session) {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        assertEquals(CustomUserProfileProvider.class.getName(), provider.getClass().getName());
        assertTrue(provider instanceof  CustomUserProfileProvider);
        assertEquals("custom-user-profile", provider.getComponentModel().getProviderId());
    }
    
    @Test
    public void testInvalidConfiguration() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) CustomUserProfileTest::testInvalidConfiguration);
    }

    private static void testInvalidConfiguration(KeycloakSession session) {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

        try {
            provider.setConfiguration("{\"validateConfigAttribute\": true}");
            fail("Should fail validation");
        } catch (ComponentValidationException ve) {
            // OK
        }
    }

    @Test
    public void testConfigurationChunks() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) CustomUserProfileTest::testConfigurationChunks);
    }

    private static void testConfigurationChunks(KeycloakSession session) throws IOException {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);
        ComponentModel component = provider.getComponentModel();

        assertNotNull(component);

        String newConfig = generateLargeProfileConfig();

        provider.setConfiguration(newConfig);

        component = provider.getComponentModel();

        // assert config is persisted in 2 pieces
        Assert.assertEquals("2", component.get(DeclarativeUserProfileProvider.UP_PIECES_COUNT_COMPONENT_CONFIG_KEY));
        // assert config is returned correctly
        Assert.assertEquals(newConfig, provider.getConfiguration());
    }


    @Test
    public void testDefaultConfig() {
        getTestingClient().server(TEST_REALM_NAME).run((RunOnServer) CustomUserProfileTest::testDefaultConfig);
    }

    private static void testDefaultConfig(KeycloakSession session) {
        DeclarativeUserProfileProvider provider = getDynamicUserProfileProvider(session);

        // reset configuration to default
        provider.setConfiguration(null);

        Map<String, Object> attributes = new HashMap<>();

        // ensure correct entered values can be validated
        attributes.put(UserModel.USERNAME, "jdoeusername");
        attributes.put(UserModel.FIRST_NAME, "John");
        attributes.put(UserModel.LAST_NAME, "Doe");
        attributes.put(UserModel.EMAIL, "jdoe@acme.org");

        UserProfile profile = provider.create(UserProfileContext.UPDATE_PROFILE, attributes);
        profile.validate();
    }

}
