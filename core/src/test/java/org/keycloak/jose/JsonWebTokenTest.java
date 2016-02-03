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
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by st on 20.08.15.
 */
public class JsonWebTokenTest {

    @Test
    public void testAudSingle() throws IOException {
        String single = "{ \"aud\": \"test\" }";
        JsonWebToken s = JsonSerialization.readValue(single, JsonWebToken.class);
        assertArrayEquals(new String[] { "test" }, s.getAudience());
    }

    @Test
    public void testAudArray() throws IOException {
        String single = "{ \"aud\": [\"test\"] }";
        JsonWebToken s = JsonSerialization.readValue(single, JsonWebToken.class);
        assertArrayEquals(new String[]{"test"}, s.getAudience());
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

}
