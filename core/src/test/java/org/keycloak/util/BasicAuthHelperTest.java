package org.keycloak.util;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
}
