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

package org.keycloak.authentication.authenticators.client;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;

import org.junit.Assert;
import org.junit.Test;

public class AbstractBaseJWTValidatorTest {

    @Test
    public void rejectExpiredAssertionWithoutIatEvenWhenWithinClockSkew() {
        JsonWebToken token = new JsonWebToken();
        token.exp(Time.currentTime() - 5L);
        RecordingValidator validator = validatorFor(token);

        Assert.assertFalse(validator.validateTokenActive(15, 300, true));
        Assert.assertEquals("Token is not active", validator.lastFailure);
    }

    @Test
    public void rejectExpiredAssertionWithIatEvenWhenWithinClockSkew() {
        JsonWebToken token = new JsonWebToken();
        token.iat(Time.currentTime() - 10L);
        token.exp(Time.currentTime() - 5L);
        RecordingValidator validator = validatorFor(token);

        Assert.assertFalse(validator.validateTokenActive(15, 300, true));
        Assert.assertEquals("Token is not active", validator.lastFailure);
    }

    @Test
    public void acceptUnexpiredAssertionWithoutIatWhenReusePermitted() {
        JsonWebToken token = new JsonWebToken();
        token.exp(Time.currentTime() + 60L);
        RecordingValidator validator = validatorFor(token);

        Assert.assertTrue(validator.validateTokenActive(15, 300, true));
        Assert.assertNull(validator.lastFailure);
    }

    @Test
    public void rejectAssertionWithoutIatWhenExpirationTooFarInTheFuture() {
        JsonWebToken token = new JsonWebToken();
        token.exp(Time.currentTime() + 400L);
        RecordingValidator validator = validatorFor(token);

        Assert.assertFalse(validator.validateTokenActive(15, 300, true));
        Assert.assertEquals("Token expiration is too far in the future and iat claim not present in token",
                validator.lastFailure);
    }

    @Test
    public void requireJtiWhenReuseIsNotPermitted() {
        JsonWebToken token = new JsonWebToken();
        token.exp(Time.currentTime() + 60L);
        RecordingValidator validator = validatorFor(token);

        Assert.assertFalse(validator.validateTokenActive(15, 300, false));
        Assert.assertEquals("Token jti claim is required", validator.lastFailure);
    }

    private static RecordingValidator validatorFor(JsonWebToken token) {
        ClientAssertionState state = new ClientAssertionState(null, null, null, token);
        return new RecordingValidator(null, state);
    }

    private static final class RecordingValidator extends AbstractBaseJWTValidator {
        private String lastFailure;

        private RecordingValidator(KeycloakSession session, ClientAssertionState clientAssertionState) {
            super(session, clientAssertionState);
        }

        @Override
        protected void failureCallback(String errorDescription) {
            lastFailure = errorDescription;
        }
    }
}
