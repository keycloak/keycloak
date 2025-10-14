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

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class ArrayElementSerializationTest {

    @Before
    public void setUp() throws Exception {
        SdJwtUtils.arrayEltSpaced = false;
    }

    @After
    public void tearDown() throws Exception {
        SdJwtUtils.arrayEltSpaced = true;
    }

    @Test
    public void testToBase64urlEncoded() {
        // Create an instance of UndisclosedArrayElement with the specified fields
        // "lklxF5jMYlGTPUovMNIvCA", "FR"
        UndisclosedArrayElement arrayElementDisclosure = UndisclosedArrayElement.builder()
                .withSalt(new SdJwtSalt("lklxF5jMYlGTPUovMNIvCA"))
                .withArrayElement(new TextNode("FR")).build();

        // Expected Base64 URL encoded string
        String expected = "WyJsa2x4RjVqTVlsR1RQVW92TU5JdkNBIiwiRlIiXQ";

        // Assert that the base64 URL encoded string from the object matches the
        // expected string
        assertEquals(expected, arrayElementDisclosure.getDisclosureString());
    }
}
