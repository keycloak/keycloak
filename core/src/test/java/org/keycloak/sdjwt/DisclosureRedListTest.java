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
package org.keycloak.sdjwt;

import org.junit.Test;

public class DisclosureRedListTest {

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInObjectClaim() {
        DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("vct")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInArrayClaim() {
        DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedArrayElt("iat", 0, "2GLC42sKQveCfGfryNRN9w")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInDecoyArrayClaim() {
        DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withDecoyArrayElt("exp", 0, "2GLC42sKQveCfGfryNRN9w")
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedIss() {
        DisclosureSpec.builder().withUndisclosedClaim("iss").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedInObjectNbf() {
        DisclosureSpec.builder().withUndisclosedClaim("nbf").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedCnf() {
        DisclosureSpec.builder().withUndisclosedClaim("cnf").build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultRedListedStatus() {
        DisclosureSpec.builder().withUndisclosedClaim("status").build();
    }
}
