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
package org.keycloak.social.twitter;

import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;
import twitter4j.RequestToken;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TwitterIdentityProviderTest {

    @Test
    public void shouldDecodeRequestToken() throws Exception {
        RequestToken requestToken = createRequestToken();

        RequestToken decoded = TwitterIdentityProvider.base64DecodeRequestToken(serialize(requestToken));

        assertEquals(requestToken, decoded);
    }

    @Test
    public void shouldRejectUnexpectedSerializedClass() throws Exception {
        String serialized = serialize(new UnexpectedObject());

        assertThrows(InvalidClassException.class,
                () -> TwitterIdentityProvider.base64DecodeRequestToken(serialized));
    }

    private static RequestToken createRequestToken() throws Exception {
        // RequestToken has no public constructor; populate responseStr to match tokens created from an HTTP response.
        Constructor<RequestToken> constructor = RequestToken.class.getDeclaredConstructor(
                String.class, String.class, String.class, String.class);
        constructor.setAccessible(true);
        RequestToken requestToken = constructor.newInstance(
                "request-token", "request-secret", "https://example.com/authorize", "https://example.com/authenticate");

        Field response = RequestToken.class.getSuperclass().getDeclaredField("responseStr");
        response.setAccessible(true);
        response.set(requestToken, new String[] { "oauth_token=request-token", "oauth_token_secret=request-secret" });
        return requestToken;
    }

    private static String serialize(Serializable value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutput = new ObjectOutputStream(Base64.getEncoder().wrap(output))) {
            objectOutput.writeObject(value);
        }
        return output.toString(StandardCharsets.US_ASCII);
    }

    private static final class UnexpectedObject implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
