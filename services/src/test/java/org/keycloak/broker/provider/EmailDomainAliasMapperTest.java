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
package org.keycloak.broker.provider;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for {@link EmailDomainAliasMapper}.
 */
public class EmailDomainAliasMapperTest {

    private static final List<String> ALIASES = List.of("example.org");

    @Test
    public void testAliasDomainIsRewrittenToCanonical() {
        assertEquals("jane.doe@example.com", EmailDomainAliasMapper.canonicalize("jane.doe@example.org", "example.com", ALIASES));
        assertEquals("Jane.Doe@example.com", EmailDomainAliasMapper.canonicalize("Jane.Doe@Example.ORG", "example.com", ALIASES));
    }

    @Test
    public void testOtherAddressesAreUnchanged() {
        assertEquals("jane.doe@example.com", EmailDomainAliasMapper.canonicalize("jane.doe@example.com", "example.com", ALIASES));
        assertEquals("jane@personal.example", EmailDomainAliasMapper.canonicalize("jane@personal.example", "example.com", ALIASES));
        assertEquals("a@notexample.org", EmailDomainAliasMapper.canonicalize("a@notexample.org", "example.com", ALIASES));
        assertEquals("a@sub.example.org", EmailDomainAliasMapper.canonicalize("a@sub.example.org", "example.com", ALIASES));
        assertEquals("not-an-email", EmailDomainAliasMapper.canonicalize("not-an-email", "example.com", ALIASES));
        assertNull(EmailDomainAliasMapper.canonicalize(null, "example.com", ALIASES));
    }

    @Test
    public void testNoCanonicalDomainMeansNoRewrite() {
        assertEquals("jane.doe@example.org", EmailDomainAliasMapper.canonicalize("jane.doe@example.org", " ", ALIASES));
        assertEquals("jane.doe@example.org", EmailDomainAliasMapper.canonicalize("jane.doe@example.org", null, ALIASES));
    }

    private static IdentityProviderMapperModel mapperModel() {
        Map<String, String> config = new HashMap<>();
        config.put(EmailDomainAliasMapper.CANONICAL_DOMAIN, "example.com");
        config.put(EmailDomainAliasMapper.ALIAS_DOMAINS, "example.org, other.example");
        IdentityProviderMapperModel mapperModel = new IdentityProviderMapperModel();
        mapperModel.setConfig(config);
        return mapperModel;
    }

    @Test
    public void testPreprocessRewritesContextEmail() {
        IdentityProviderMapperModel mapperModel = mapperModel();

        IdentityProviderModel idp = new IdentityProviderModel();
        idp.setEnabled(true);
        BrokeredIdentityContext context = new BrokeredIdentityContext("id", idp);
        context.setEmail("jane.doe@example.org");
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertEquals("jane.doe@example.com", context.getEmail());

        context.setEmail("x@other.example");
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertEquals("x@example.com", context.getEmail());

        context.setEmail(null);
        new EmailDomainAliasMapper().preprocessFederatedIdentity(null, null, mapperModel, context);
        assertNull(context.getEmail());
    }

    // --- Sync mode force: existing users

    @Test
    public void testUpdateMovesStoredAliasAddressToCanonicalDomain() {
        FakeUser user = new FakeUser("1", "jane.doe@example.org");
        new EmailDomainAliasMapper().updateBrokeredUser(session(user), null, user.model(), mapperModel(), null);
        // Only setEmail is called: FakeUser rejects any other change, such as the email verified flag.
        assertEquals("jane.doe@example.com", user.email);
    }

    @Test
    public void testUpdateLeavesOtherAddressesUnchanged() {
        for (String email : new String[] { "jane.doe@example.com", "jane@personal.example", null }) {
            FakeUser user = new FakeUser("1", email);
            new EmailDomainAliasMapper().updateBrokeredUser(session(user), null, user.model(), mapperModel(), null);
            assertEquals(email, user.email);
            assertEquals(0, user.emailChanges);
        }
    }

    @Test
    public void testUpdateDoesNotTakeAnotherUsersAddress() {
        FakeUser user = new FakeUser("1", "jane.doe@example.org");
        FakeUser other = new FakeUser("2", "jane.doe@example.com");
        new EmailDomainAliasMapper().updateBrokeredUser(session(user, other), null, user.model(), mapperModel(), null);
        assertEquals("jane.doe@example.org", user.email);
        assertEquals(0, user.emailChanges);
    }

    /**
     * Session whose user provider finds {@code users} by email.
     */
    private static KeycloakSession session(FakeUser... users) {
        UserProvider userProvider = proxy(UserProvider.class, (method, args) -> {
            if (method.equals("getUserByEmail")) {
                String email = ((String) args[1]).toLowerCase(Locale.ROOT);
                for (FakeUser user : users) {
                    if (user.email != null && user.email.toLowerCase(Locale.ROOT).equals(email)) {
                        return user.model();
                    }
                }
                return null;
            }
            throw new UnsupportedOperationException(method);
        });
        return proxy(KeycloakSession.class, (method, args) -> {
            if (method.equals("users")) {
                return userProvider;
            }
            throw new UnsupportedOperationException(method);
        });
    }

    private static class FakeUser {
        final String id;
        String email;
        int emailChanges;

        FakeUser(String id, String email) {
            this.id = id;
            this.email = email;
        }

        UserModel model() {
            return proxy(UserModel.class, (method, args) -> {
                switch (method) {
                    case "getId":
                        return id;
                    case "getUsername":
                        return "user-" + id;
                    case "getEmail":
                        return email;
                    case "setEmail":
                        email = (String) args[0];
                        emailChanges++;
                        return null;
                    default:
                        throw new UnsupportedOperationException(method);
                }
            });
        }
    }

    private interface Handler {
        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(EmailDomainAliasMapperTest.class.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> handler.invoke(method.getName(), args)));
    }
}
