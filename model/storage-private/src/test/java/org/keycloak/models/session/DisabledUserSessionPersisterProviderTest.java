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

package org.keycloak.models.session;

import java.lang.reflect.Proxy;
import java.util.Properties;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.models.KeycloakSessionFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DisabledUserSessionPersisterProviderTest {
    @Before
    public void initializeProfile() {
        Profile.defaults();
    }

    @After
    public void resetProfile() {
        Profile.reset();
    }

    @Test
    public void selectedDisabledPersisterFailsDuringStartup() {
        var provider = new DisabledUserSessionPersisterProvider();
        var error = assertThrows(IllegalStateException.class, () -> provider.postInit(factory(provider)));
        assertTrue(error.getMessage().contains("Remove --spi-user-session-persister--provider=disabled"));
    }

    @Test
    public void unselectedDisabledPersisterDoesNotPreventStartup() {
        new DisabledUserSessionPersisterProvider().postInit(factory(null));
    }

    @Test
    public void volatileSessionsAllowDisabledPersister() {
        var properties = new Properties();
        properties.setProperty(PropertiesProfileConfigResolver.getPropertyKey(Profile.Feature.PERSISTENT_USER_SESSIONS), "disabled");
        Profile.configure(new PropertiesProfileConfigResolver(properties));
        var provider = new DisabledUserSessionPersisterProvider();
        provider.postInit(factory(provider));
        assertSame(provider, provider.create(null));
        assertFalse(UserSessionPersisterProviderFactory.isPersistenceEnabled(factory(provider)));
    }

    @Test
    public void missingPersisterIsNotTreatedAsCacheOnlyMode() {
        assertThrows(IllegalStateException.class, () -> UserSessionPersisterProviderFactory.isPersistenceEnabled(factory(null)));
    }

    private static KeycloakSessionFactory factory(UserSessionPersisterProviderFactory selected) {
        return (KeycloakSessionFactory) Proxy.newProxyInstance(KeycloakSessionFactory.class.getClassLoader(),
                new Class<?>[] { KeycloakSessionFactory.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getProviderFactory") && args.length == 1
                            && args[0] == UserSessionPersisterProvider.class) {
                        return selected;
                    }
                    throw new AssertionError("Unexpected session factory call: " + method);
                });
    }
}
