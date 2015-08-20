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
