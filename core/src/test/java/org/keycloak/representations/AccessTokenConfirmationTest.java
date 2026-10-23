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

package org.keycloak.representations;

import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Test;

public class AccessTokenConfirmationTest {

    @Test
    public void otherConfirmationMembers() throws Exception {
        AccessToken token = JsonSerialization.readValue(
                "{\"sub\":\"user\",\"cnf\":{\"x5t#S256\":\"abc\",\"x5t#custom\":\"def\"}}", AccessToken.class);

        Assert.assertEquals("abc", token.getConfirmation().getCertThumbprint());
        Assert.assertEquals("def", token.getConfirmation().getOtherClaims().get("x5t#custom"));

        AccessToken copy = JsonSerialization.readValue(JsonSerialization.writeValueAsString(token), AccessToken.class);
        Assert.assertEquals("abc", copy.getConfirmation().getCertThumbprint());
        Assert.assertEquals("def", copy.getConfirmation().getOtherClaims().get("x5t#custom"));
    }

    @Test
    public void noOtherConfirmationMembers() throws Exception {
        AccessToken.Confirmation cnf = new AccessToken.Confirmation();
        cnf.setCertThumbprint("abc");
        Assert.assertEquals("{\"x5t#S256\":\"abc\"}", JsonSerialization.writeValueAsString(cnf));
    }
}
