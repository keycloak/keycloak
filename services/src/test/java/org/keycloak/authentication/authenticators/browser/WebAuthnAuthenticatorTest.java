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
package org.keycloak.authentication.authenticators.browser;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class WebAuthnAuthenticatorTest {

    @Test
    public void storableUserIdIsAccepted() {
        assertThat(WebAuthnAuthenticator.isStorableUserId("f:1234:alice"), is(true));
        assertThat(WebAuthnAuthenticator.isStorableUserId("6b8f0c1e-6b8f-0c1e-6b8f-0c1e6b8f0c1e"), is(true));
        assertThat(WebAuthnAuthenticator.isStorableUserId(""), is(true));
    }

    @Test
    public void userIdWithNulCharacterIsRejected() {
        assertThat(WebAuthnAuthenticator.isStorableUserId("\u0000"), is(false));
        assertThat(WebAuthnAuthenticator.isStorableUserId("a\u0000b"), is(false));
        assertThat(WebAuthnAuthenticator.isStorableUserId("alice\u0000"), is(false));
    }

    @Test
    public void userHandleDecodingToNulIsRejected() {
        // the user handle bytes 61 00 62 00 from the report decode to "a\0b\0"
        String decoded = new String(new byte[] {0x61, 0x00, 0x62, 0x00}, StandardCharsets.UTF_8);
        assertThat(WebAuthnAuthenticator.isStorableUserId(decoded), is(false));
    }
}
