package org.keycloak;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JsonParserTest {

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
