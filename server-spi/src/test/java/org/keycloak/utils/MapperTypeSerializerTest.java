/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.utils;

import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.MultivaluedMap;
import org.keycloak.models.utils.MapperTypeSerializer;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapperTypeSerializerTest {

    @Test
    public void testBasicSerializeAndDeserialize() {
        // Serialize
        MultivaluedMap<String, String> simpleMap = new MultivaluedHashMap<>() {
            {
                putSingle("attr1", "Apple");
                putSingle("attr2", "Orange");
            }
        };
        String s = MapperTypeSerializer.serialize(simpleMap);

        // Check after deserialize, it is equal to serialized
        Map<String, List<String>> deserialized = MapperTypeSerializer.deserialize(s);
        Assert.assertEquals(simpleMap, deserialized);

        // Deserialize from String
        deserialized = MapperTypeSerializer.deserialize("[{\"key\":\"attr2\",\"value\":\"Orange\"},{\"key\":\"attr1\",\"value\":\"Apple\"}]");
        Assert.assertEquals(simpleMap, deserialized);
    }

    @Test
    public void testMultivaluedSerializeAndDeserialize() {
        // Deserialize with some multivalued value
        Map<String, List<String>> deserialized = MapperTypeSerializer.deserialize("[{\"key\":\"attr2\",\"value\":\"Orange\"},{\"key\":\"attr1\",\"value\":\"Apple\"},{\"key\":\"attr2\",\"value\":\"Peach\"}]");
        Assert.assertEquals(deserialized.get("attr1").size(), 1);
        Assert.assertEquals(deserialized.get("attr1").get(0), "Apple");
        Assert.assertEquals(deserialized.get("attr2").size(), 2);
        Assert.assertTrue(deserialized.get("attr2").contains("Orange"));
        Assert.assertTrue(deserialized.get("attr2").contains("Peach"));
        Assert.assertFalse(deserialized.get("attr2").contains("Apple"));

        // Serialize and deserialize again from String and check it is same value
        String s = MapperTypeSerializer.serialize(deserialized);
        Assert.assertEquals(MapperTypeSerializer.deserialize(s), deserialized);
    }




}
