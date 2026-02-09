/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.authz;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.authorization.attribute.Attributes;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Test;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AttributeTest {

    @Test
    public void testManageAttributes() throws ParseException {
        Map<String, Collection<String>> map = new HashedMap();

        map.put("integer", asList("1"));
        map.put("long", asList("" + Long.MAX_VALUE));
        map.put("string", asList("some string"));
        map.put("date", asList("12/12/2016"));
        map.put("ip_network_address", asList("127.0.0.1"));
        map.put("host_network_address", asList("localhost"));
        map.put("multi_valued", asList("1", "2", "3", "4"));

        Attributes attributes = Attributes.from(map);

        map.keySet().forEach(new Consumer<String>() {
            @Override
            public void accept(String name) {
                assertTrue(attributes.exists(name));
            }
        });

        assertFalse(attributes.exists("not_found"));
        assertTrue(attributes.containsValue("integer", "1"));
        assertTrue(attributes.containsValue("multi_valued", "3"));

        assertEquals(1, attributes.getValue("multi_valued").asInt(0));
        assertEquals(4, attributes.getValue("multi_valued").asInt(3));

        assertEquals(new SimpleDateFormat("dd/MM/yyyy").parse("12/12/2016"), attributes.getValue("date").asDate(0, "dd/MM/yyyy"));

        assertEquals("127.0.0.1", attributes.getValue("ip_network_address").asInetAddress(0).getHostAddress());
        assertEquals("localhost", attributes.getValue("host_network_address").asInetAddress(0).getHostName());
    }
}
