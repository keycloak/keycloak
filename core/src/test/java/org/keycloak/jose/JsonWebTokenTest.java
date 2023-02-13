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

package org.keycloak.jose;

import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by st on 20.08.15.
 */
public class JsonWebTokenTest {

    @Test
    public void testAudSingle() throws IOException {
        String single = "{ \"aud\": \"test\" }";
        JsonWebToken s = JsonSerialization.readValue(single, JsonWebToken.class);
        assertArrayEquals(new String[]{"test"}, s.getAudience());
    }

    @Test
    public void testAudArray() throws IOException {
        String single = "{ \"aud\": [\"test\"] }";
        JsonWebToken s = JsonSerialization.readValue(single, JsonWebToken.class);
        assertArrayEquals(new String[]{"test"}, s.getAudience());
    }

    @Test
    public void testAddAudience() {
        // Token with no audience
        JsonWebToken s = new JsonWebToken();
        s.addAudience("audience-1");
        assertArrayEquals(new String[] { "audience-1"}, s.getAudience());

        // Add to existing
        s.addAudience("audience-2");
        assertArrayEquals(new String[]{"audience-1", "audience-2"}, s.getAudience());

        s.addAudience("audience-3");
        assertArrayEquals(new String[]{"audience-1", "audience-2", "audience-3"}, s.getAudience());

        // Add existing. Shouldn't be added as it's already there
        s.addAudience("audience-2");
        assertArrayEquals(new String[]{"audience-1", "audience-2", "audience-3"}, s.getAudience());
    }

    @Test
    public void test() throws IOException {
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.audience("test");
        assertTrue(JsonSerialization.writeValueAsPrettyString(jsonWebToken).contains("\"aud\" : \"test\""));
    }

    @Test
    public void testArray() throws IOException {
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.audience("test", "test2");
        assertTrue(JsonSerialization.writeValueAsPrettyString(jsonWebToken).contains("\"aud\" : [ \"test\", \"test2\" ]"));
    }

    @Test
    public void isActiveReturnFalseWhenBeforeTimeInFuture() {
        int currentTime = Time.currentTime();
        int futureTime = currentTime + 10;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.notBefore(futureTime);
        assertFalse(jsonWebToken.isActive());
    }

    @Test
    public void isActiveReturnTrueWhenBeforeTimeInPast() {
        int currentTime = Time.currentTime();
        int pastTime = currentTime - 10;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.notBefore(pastTime);
        assertTrue(jsonWebToken.isActive());
    }

    @Test
    public void isActiveShouldReturnTrueWhenBeforeTimeInFutureWithinTimeSkew() {
        int notBeforeTime = Time.currentTime() + 5;
        int allowedClockSkew = 10;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.notBefore(notBeforeTime);
        assertTrue(jsonWebToken.isActive(allowedClockSkew));
    }

    @Test
    public void isActiveShouldReturnFalseWhenWhenBeforeTimeInFutureOutsideTimeSkew() {
        int notBeforeTime = Time.currentTime() + 10;
        int allowedClockSkew = 5;
        JsonWebToken jsonWebToken = new JsonWebToken();
        jsonWebToken.notBefore(notBeforeTime);
        assertFalse(jsonWebToken.isActive(allowedClockSkew));
    }

}
