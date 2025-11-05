package org.keycloak.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class BasicAuthHelperTest {

    @Test
    public void createHeader() {
        String username = "Aladdin";
        String password = "open sesameopen sesameopen sesameopen sesameopen sesameopen sesame";

        String actual = BasicAuthHelper.createHeader(username, password);
        String expect = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZW9wZW4gc2VzYW1lb3BlbiBzZXNhbWVvcGVuIHNlc2FtZW9wZW4gc2VzYW1lb3BlbiBzZXNhbWU=";

        assertEquals(expect, actual);
    }

    @Test
    public void parseHeader() {
        String username = "Aladdin";
        String password = "open sesameopen sesameopen sesameopen sesameopen sesameopen sesameopen sesame";

        String header = BasicAuthHelper.createHeader(username, password);
        String[] actual = BasicAuthHelper.parseHeader(header);

        assertArrayEquals(new String[] {username, password}, actual);
    }

    @Test
    public void rfc6749_createHeader() {
        String username = "user";
        String password = "secret/with=special?character";

        String actual = BasicAuthHelper.RFC6749.createHeader(username, password);
        String expect = "Basic dXNlcjpzZWNyZXQlMkZ3aXRoJTNEc3BlY2lhbCUzRmNoYXJhY3Rlcg==";

        assertEquals(expect, actual);
    }

    @Test
    public void rfc6749_parseHeader() {
        String username = "user";
        String password = "secret/with=special?character";

        String header = BasicAuthHelper.createHeader(username, password);
        String[] actual = BasicAuthHelper.parseHeader(header);

        assertArrayEquals(new String[] {username, password}, actual);
    }

    @Test
    public void rfc6749_parseHeader_withSpecialCharacters_notUrlEncoded() {
        String clientId = "my-client";
        String clientSecret = "#NdhMw3!?:R+7_8{EsKU44W(";

        String header = BasicAuthHelper.createHeader(clientId, clientSecret);

        String[] actual = BasicAuthHelper.RFC6749.parseHeader(header);

        assertNotNull("Parsing should not return null", actual);
        assertEquals("Client ID should match", clientId, actual[0]);
        assertEquals("Client secret should match", clientSecret, actual[1]);
    }

    @Test
    public void rfc6749_parseHeader_withSpecialCharacters_urlEncoded() {
        String clientId = "my-client";
        String clientSecret = "#NdhMw3!?:R+7_8{EsKU44W(";

        String header = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);

        String[] actual = BasicAuthHelper.RFC6749.parseHeader(header);

        assertNotNull("Parsing should not return null", actual);
        assertEquals("Client ID should match", clientId, actual[0]);
        assertEquals("Client secret should match", clientSecret, actual[1]);
    }

    @Test
    public void rfc6749_parseHeader_withPercentSign_notUrlEncoded() {
        String clientId = "my-client";
        String clientSecret = "abc%def";

        String header = BasicAuthHelper.createHeader(clientId, clientSecret);

        String[] actual = BasicAuthHelper.RFC6749.parseHeader(header);

        assertNotNull("Parsing should not return null", actual);
        assertEquals("Client ID should match", clientId, actual[0]);
        assertEquals("Client secret should preserve % character", clientSecret, actual[1]);
    }

    @Test
    public void rfc6749_parseHeader_withValidUrlEncoding() {
        String clientId = "my-client";
        String clientSecret = "abc+def";

        String header = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);

        String[] actual = BasicAuthHelper.RFC6749.parseHeader(header);

        assertNotNull("Parsing should not return null", actual);
        assertEquals("Client ID should match", clientId, actual[0]);
        assertEquals("Client secret should match (+ preserved)", clientSecret, actual[1]);
    }

    @Test
    public void rfc6749_parseHeader_withMixedSpecialCharacters() {
        String clientId = "test-client";
        String clientSecret = "p@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?";

        String header = BasicAuthHelper.createHeader(clientId, clientSecret);
        String[] actual = BasicAuthHelper.RFC6749.parseHeader(header);

        assertNotNull("Parsing should not return null", actual);
        assertEquals("Client ID should match", clientId, actual[0]);
        assertEquals("Client secret with special chars should match", clientSecret, actual[1]);
    }
}
