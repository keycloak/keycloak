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

package org.keycloak.authorization.client;

import java.io.IOException;
import java.io.InputStream;

import org.keycloak.representations.adapters.config.AdapterConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JsonParserTest {

    @Test
    public void testParsingSystemProps() throws IOException {
        System.setProperty("my.host", "foo");
        System.setProperty("con.pool.size", "200");
        System.setProperty("allow.any.hostname", "true");
        System.setProperty("socket.timeout.millis", "6000");
        System.setProperty("connection.timeout.millis", "7000");
        System.setProperty("connection.ttl.millis", "500");

        InputStream is = getClass().getClassLoader().getResourceAsStream("keycloak.json");

        ObjectMapper mapper = new ObjectMapper(new SystemPropertiesJsonParserFactory());
        AdapterConfig config = mapper.readValue(is, AdapterConfig.class);
        Assert.assertEquals("http://foo:8080/auth", config.getAuthServerUrl());
        Assert.assertEquals("external", config.getSslRequired());
        Assert.assertEquals("angular-product${non.existing}", config.getResource());
        Assert.assertTrue(config.isPublicClient());
        Assert.assertTrue(config.isAllowAnyHostname());
        Assert.assertEquals(100, config.getCorsMaxAge());
        Assert.assertEquals(200, config.getConnectionPoolSize());
        Assert.assertEquals(6000L, config.getSocketTimeout());
        Assert.assertEquals(7000L, config.getConnectionTimeout());
        Assert.assertEquals(500L, config.getConnectionTTL());
    }
}
