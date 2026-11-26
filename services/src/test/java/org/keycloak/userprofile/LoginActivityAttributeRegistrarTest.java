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
package org.keycloak.userprofile;

import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;

import org.junit.Assert;
import org.junit.Test;

public class LoginActivityAttributeRegistrarTest {

    @Test
    public void simpleClientIdKeepsExactAuditorCompatibleName() {
        // Plain alphanumeric client IDs are the common case and must stay byte-for-byte identical to
        // what the community keycloak-auditor plugin already writes, so existing deployments need no migration.
        Assert.assertEquals("aud_usr_last-login_my-client",
                LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-1", "my-client")));
        Assert.assertEquals("aud_usr_last-login_my.client_01",
                LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-2", "my.client_01")));
    }

    @Test
    public void urlLikeClientIdIsSanitizedInsteadOfProducingAnInvalidAttributeName() {
        // SAML entity IDs/URLs contain ':' and '/', which UPConfigUtils.ATTRIBUTE_NAME_PATTERN rejects.
        // Registering such a name used to make the whole realm's user-profile config invalid, breaking
        // logins for every client in the realm - see https://github.com/keycloak/keycloak/pull/55586.
        String name = LoginActivityAttributeRegistrar.perClientAttributeName(
                client("internal-1", "http://localhost:8280/sales-post/"));

        Assert.assertTrue(name.matches("aud_usr_last-login_[a-zA-Z0-9._-]+"));
    }

    @Test
    public void sanitizedNameIsStableAcrossCalls() {
        ClientModel client = client("internal-1", "http://localhost:8280/sales-post/");

        Assert.assertEquals(LoginActivityAttributeRegistrar.perClientAttributeName(client),
                LoginActivityAttributeRegistrar.perClientAttributeName(client));
    }

    @Test
    public void differentClientsWithSameUnsafeClientIdSanitizationDoNotCollide() {
        // "a/b" and "a:b" both sanitize their single unsafe character down to the identical "a_b"
        // prefix. Hashing each client's unique internal ID (rather than its human-chosen clientId)
        // means two different clients that happen to sanitize the same way still get distinct names.
        String first = LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-1", "a/b"));
        String second = LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-2", "a:b"));

        Assert.assertNotEquals(first, second);
    }

    @Test
    public void veryLongClientIdProducesAnAttributeNameWithinTheColumnLimit() {
        // USER_ATTRIBUTE.NAME is a VARCHAR(255) column. A valid 255-character client ID would otherwise
        // produce a 274-character attribute name, making every login for that client fail to persist.
        String longClientId = "a".repeat(255);
        String name = LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-1", longClientId));

        Assert.assertTrue("name was " + name.length() + " characters", name.length() <= 255);
    }

    @Test
    public void veryLongClientIdsThatDifferOnlyAtTheEndStillProduceDistinctNames() {
        String longClientId = "a".repeat(255);
        String otherLongClientId = "a".repeat(254) + "b";

        String first = LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-1", longClientId));
        String second = LoginActivityAttributeRegistrar.perClientAttributeName(client("internal-2", otherLongClientId));

        Assert.assertNotEquals(first, second);
    }

    /**
     * A {@link ClientModel} interface has far too many methods to hand-implement for a test that only
     * needs {@code getId()}/{@code getClientId()}; a JDK dynamic proxy returning a type-appropriate
     * default for everything else avoids that without pulling in a mocking library.
     */
    private static ClientModel client(String internalId, String clientId) {
        return (ClientModel) Proxy.newProxyInstance(
                LoginActivityAttributeRegistrarTest.class.getClassLoader(),
                new Class<?>[]{ClientModel.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getId" -> internalId;
                    case "getClientId" -> clientId;
                    default -> defaultValueFor(method.getReturnType());
                });
    }

    private static Object defaultValueFor(Class<?> returnType) {
        if (returnType == Stream.class) {
            return Stream.empty();
        }
        if (!returnType.isPrimitive() || returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        return 0;
    }
}
