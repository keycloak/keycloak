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

package org.keycloak.url;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.utils.ScopeUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class HostnameV2ProviderFactoryTest {

    @Test
    public void hostnameUrlValidationTest() throws IOException{
        assertHostname("https://my-example.com/auth.this", true);
        assertHostname("https://my-example.com:8080", true);
        assertHostname("https://my-example.com/auth%20this", true);
        assertHostname("my-example.com", true);
        assertHostname("192.196.0.0", true);
        assertHostname("[2001:0000:130F:0000:0000:09C0:876A:130B]", true);
        
        assertHostname("https://my-example.com?auth.this", false);
        assertHostname("my-example.com/auth.this", false);
        assertHostname("https://my-example.com:8080#fragment", false);
        assertHostname("my-example.com:8080", false);
        assertHostname("ldap://my-example.com/auth%20this", false);
        assertHostname("?my-example.com", false);
        assertHostname("192.196.0.5555", false);
    }
    
    @Test
    public void hostnameUrlExpected() throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("hostname", "short");
        values.put("hostname-admin", "https://other");
        HostnameV2ProviderFactory factory = new HostnameV2ProviderFactory();
        assertEquals("hostname must be set to a URL when hostname-admin is set",
                assertThrows(IllegalArgumentException.class, () -> factory.init(ScopeUtil.createScope(values))).getMessage());
    }

    private void assertHostname(String hostname, boolean valid) {
        Map<String, String> values = new HashMap<>();
        values.put("hostname", hostname);
        HostnameV2ProviderFactory factory = new HostnameV2ProviderFactory();
        try {
            factory.init(ScopeUtil.createScope(values));
            assertTrue(valid);
        } catch (IllegalArgumentException e) {
            assertFalse(valid);
        }
    }

}
