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
package org.keycloak.social.steam;

import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.UserModel;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test for {@link org.keycloak.social.steam.SteamUserAttributeMapper}
 * and its profile name-splitting logic.
 *
 * @author André Rocha
 * @author João Viegas
 */
public class SteamUserAttributeMapperTest {

    /**
     * Instantiates a dynamic dynamic proxy implementation of UserModel to trace mutations.
     */
    private UserModel createDummyUser(Map<String, String> attributeTracker) {
        return (UserModel) Proxy.newProxyInstance(
                UserModel.class.getClassLoader(),
                new Class[]{UserModel.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setFirstName")) attributeTracker.put("firstName", (String) args[0]);
                    if (method.getName().equals("setLastName")) attributeTracker.put("lastName", (String) args[0]);
                    if (method.getName().equals("getFirstName")) return attributeTracker.get("firstName");
                    if (method.getName().equals("getLastName")) return attributeTracker.get("lastName");
                    return null;
                }
        );
    }

    /**
     * Verify realname values containing multiple components split correctly into native fields.
     */
    @Test
    public void testSteamNameSplitter() {
        SteamUserAttributeMapper mapper = new SteamUserAttributeMapper();

        ObjectMapper jsonParser = new ObjectMapper();
        ObjectNode fakeSteamProfile = jsonParser.createObjectNode();
        fakeSteamProfile.put("realname", "Gabe Newell");

        SteamIdentityProviderConfig config = new SteamIdentityProviderConfig();
        config.setEnabled(true);
        BrokeredIdentityContext context = new BrokeredIdentityContext("123456789", config);
        context.getContextData().put(AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE, fakeSteamProfile);

        // Map to track what the mapper writes to the user
        Map<String, String> tracker = new HashMap<>();
        UserModel dummyUser = createDummyUser(tracker);

        mapper.updateBrokeredUser(null, null, dummyUser, null, context);

        Assert.assertEquals("Gabe", tracker.get("firstName"));
        Assert.assertEquals("Newell", tracker.get("lastName"));
    }

    /**
     * Verify realname values containing a single word populate first name and clear last name.
     */
    @Test
    public void testSteamNameSplitterSingleNameFallback() {
        SteamUserAttributeMapper mapper = new SteamUserAttributeMapper();

        ObjectMapper jsonParser = new ObjectMapper();
        ObjectNode fakeSteamProfile = jsonParser.createObjectNode();
        fakeSteamProfile.put("realname", "McLovin");

        SteamIdentityProviderConfig config = new SteamIdentityProviderConfig();
        config.setEnabled(true);
        BrokeredIdentityContext context = new BrokeredIdentityContext("123456789", config);
        context.getContextData().put(AbstractJsonUserAttributeMapper.CONTEXT_JSON_NODE, fakeSteamProfile);

        Map<String, String> tracker = new HashMap<>();
        UserModel dummyUser = createDummyUser(tracker);

        mapper.updateBrokeredUser(null, null, dummyUser, null, context);

        Assert.assertEquals("McLovin", tracker.get("firstName"));
        Assert.assertEquals("", tracker.get("lastName"));
    }
}
