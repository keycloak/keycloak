package org.keycloak;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JsonParserTest {

    @Test
    public void regex() throws Exception {
        Pattern p = Pattern.compile(".*(?!\\.pdf)");
        if (p.matcher("foo.pdf").matches()) {
            System.out.println(".pdf no match");
        }
        if (p.matcher("foo.txt").matches()) {
            System.out.println("foo.txt matches");

        }

    }

    @Test
    public void testOtherClaims() throws Exception {
        String json = "{ \"floatData\" : 555.5," +
                "\"boolData\": true, " +
                "\"intData\": 1234," +
                "\"array\": [ \"val\", \"val2\"] }";
        JsonWebToken token = JsonSerialization.readValue(json, JsonWebToken.class);
        System.out.println(token.getOtherClaims().get("floatData").getClass().getName());
        System.out.println(token.getOtherClaims().get("boolData").getClass().getName());
        System.out.println(token.getOtherClaims().get("intData").getClass().getName());
        System.out.println(token.getOtherClaims().get("array").getClass().getName());
    }

    @Test
    public void testUnwrap() throws Exception {
        // just experimenting with unwrapped and any properties
        IDToken test = new IDToken();
        test.getOtherClaims().put("phone_number", "978-666-0000");
        test.getOtherClaims().put("email_verified", "true");
        test.getOtherClaims().put("yo", "true");
        Map<String, String> nested = new HashMap<String, String>();
        nested.put("foo", "bar");
        test.getOtherClaims().put("nested", nested);
        String json = JsonSerialization.writeValueAsPrettyString(test);
        System.out.println(json);

        test = JsonSerialization.readValue(json, IDToken.class);
        System.out.println("email_verified property: " + test.getEmailVerified());
        System.out.println("property: " + test.getPhoneNumber());
        System.out.println("map: " + test.getOtherClaims().get("phone_number"));
        Assert.assertNotNull(test.getPhoneNumber());
        Assert.assertNotNull(test.getOtherClaims().get("yo"));
        Assert.assertNull(test.getOtherClaims().get("phone_number"));
        nested = (Map<String, String>)test.getOtherClaims().get("nested");
        Assert.assertNotNull(nested);
        Assert.assertNotNull(nested.get("foo"));
    }

    @Test
    public void testParsingSystemProps() throws IOException {
        System.setProperty("my.host", "foo");
        System.setProperty("con.pool.size", "200");
        System.setProperty("allow.any.hostname", "true");

        InputStream is = getClass().getClassLoader().getResourceAsStream("keycloak.json");

        AdapterConfig config = JsonSerialization.readValue(is, AdapterConfig.class, true);
        Assert.assertEquals("http://foo:8080/auth", config.getAuthServerUrl());
        Assert.assertEquals("external", config.getSslRequired());
        Assert.assertEquals("angular-product${non.existing}", config.getResource());
        Assert.assertTrue(config.isPublicClient());
        Assert.assertTrue(config.isAllowAnyHostname());
        Assert.assertEquals(100, config.getCorsMaxAge());
        Assert.assertEquals(200, config.getConnectionPoolSize());
    }
}
