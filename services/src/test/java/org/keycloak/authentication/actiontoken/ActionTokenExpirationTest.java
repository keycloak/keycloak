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
package org.keycloak.authentication.actiontoken;

import java.util.Collections;

import org.keycloak.authentication.actiontoken.execactions.ExecuteActionsActionToken;
import org.keycloak.authentication.actiontoken.resetcred.ResetCredentialsActionToken;
import org.keycloak.authentication.actiontoken.updateemail.UpdateEmailActionToken;
import org.keycloak.authentication.actiontoken.verifyemail.VerifyEmailActionToken;
import org.keycloak.models.DefaultActionTokenKey;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ActionTokenExpirationTest {

    // A timestamp after 2038-01-19, which does not fit into an int
    private static final long EXPIRATION_AFTER_2038 = Integer.MAX_VALUE + 300L;

    @Test
    public void testResetCredentialsActionTokenWithLongExpiration() {
        ResetCredentialsActionToken token = new ResetCredentialsActionToken("user-id", "user@example.com",
                EXPIRATION_AFTER_2038, "auth-session-id", "client-id");

        assertEquals(Long.valueOf(EXPIRATION_AFTER_2038), token.getExp());
    }

    @Test
    public void testIntExpirationStillSupported() {
        int expiration = 1_700_000_000;
        ResetCredentialsActionToken token = new ResetCredentialsActionToken("user-id", "user@example.com",
                expiration, "auth-session-id", "client-id");

        assertEquals(Long.valueOf(expiration), token.getExp());
    }

    @Test
    public void testOtherActionTokensWithLongExpiration() {
        assertEquals(Long.valueOf(EXPIRATION_AFTER_2038),
                new VerifyEmailActionToken("user-id", EXPIRATION_AFTER_2038, "auth-session-id", "user@example.com", "client-id").getExp());
        assertEquals(Long.valueOf(EXPIRATION_AFTER_2038),
                new ExecuteActionsActionToken("user-id", EXPIRATION_AFTER_2038, Collections.emptyList(), null, "client-id").getExp());
        assertEquals(Long.valueOf(EXPIRATION_AFTER_2038),
                new UpdateEmailActionToken("user-id", EXPIRATION_AFTER_2038, "old@example.com", "new@example.com", "client-id").getExp());
    }

    @Test
    public void testSerializedKeyRoundTripWithLongExpiration() {
        ResetCredentialsActionToken token = new ResetCredentialsActionToken("user-id", "user@example.com",
                EXPIRATION_AFTER_2038, "auth-session-id", "client-id");

        DefaultActionTokenKey key = DefaultActionTokenKey.from(token.serializeKey());

        assertNotNull(key);
        assertEquals(Long.valueOf(EXPIRATION_AFTER_2038), key.getExp());
        assertEquals("user-id", key.getUserId());
        assertEquals(ResetCredentialsActionToken.TOKEN_TYPE, key.getActionId());
        assertEquals(token.getActionVerificationNonce(), key.getActionVerificationNonce());
    }
}
