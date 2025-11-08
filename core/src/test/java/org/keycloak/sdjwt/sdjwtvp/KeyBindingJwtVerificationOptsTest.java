/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sdjwt.sdjwtvp;

import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;

import org.junit.Test;

public class KeyBindingJwtVerificationOptsTest {

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFail_IfKeyBindingRequired_AndNonceNotSpecified() {
        KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFail_IfKeyBindingRequired_AndNonceEmpty() {
        KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withNonce("")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildShouldFail_IfKeyBindingRequired_AndAudNotSpecified() {
        KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withNonce("12345678")
                .build();
    }

}
