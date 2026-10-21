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
package org.keycloak.protocol.oidc.mappers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.utils.JsonUtils;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 * @author hmlnarik
 */
public class OIDCAttributeMapperHelperTest {

    private ProtocolMapperModel createMappingModel(String jsonType) {
        ProtocolMapperModel mappingModel = new ProtocolMapperModel();
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.JSON_TYPE, jsonType);
        mappingModel.setConfig(config);
        return mappingModel;
    }

    @Test
    public void testMapAttributeValue_longType_validValue() {
        ProtocolMapperModel model = createMappingModel("long");
        assertEquals(123L, OIDCAttributeMapperHelper.mapAttributeValue(model, "123"));
    }

    @Test
    public void testMapAttributeValue_longType_emptyString() {
        ProtocolMapperModel model = createMappingModel("long");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, ""));
    }

    @Test
    public void testMapAttributeValue_longType_invalidString() {
        ProtocolMapperModel model = createMappingModel("long");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, "not_a_number"));
    }

    @Test
    public void testMapAttributeValue_longType_nullValue() {
        ProtocolMapperModel model = createMappingModel("long");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, null));
    }

    @Test
    public void testMapAttributeValue_intType_validValue() {
        ProtocolMapperModel model = createMappingModel("int");
        assertEquals(42, OIDCAttributeMapperHelper.mapAttributeValue(model, "42"));
    }

    @Test
    public void testMapAttributeValue_intType_emptyString() {
        ProtocolMapperModel model = createMappingModel("int");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, ""));
    }

    @Test
    public void testMapAttributeValue_intType_invalidString() {
        ProtocolMapperModel model = createMappingModel("int");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, "abc"));
    }

    @Test
    public void testMapAttributeValue_booleanType_validValue() {
        ProtocolMapperModel model = createMappingModel("boolean");
        assertEquals(true, OIDCAttributeMapperHelper.mapAttributeValue(model, "true"));
        assertEquals(false, OIDCAttributeMapperHelper.mapAttributeValue(model, "false"));
    }

    @Test
    public void testMapAttributeValue_booleanType_nonBooleanObject() {
        ProtocolMapperModel model = createMappingModel("boolean");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, 123));
    }

    @Test
    public void testMapAttributeValue_stringType_value() {
        ProtocolMapperModel model = createMappingModel("String");
        assertEquals("hello", OIDCAttributeMapperHelper.mapAttributeValue(model, "hello"));
    }

    @Test
    public void testMapAttributeValue_longType_listSingleValue() {
        ProtocolMapperModel model = createMappingModel("long");
        // non-multivalued model takes the first element
        assertEquals(123L, OIDCAttributeMapperHelper.mapAttributeValue(model, Arrays.asList("123", "456")));
    }

    @Test
    public void testMapAttributeValue_longType_listMultivalued() {
        ProtocolMapperModel model = createMappingModel("long");
        model.getConfig().put("multivalued", "true");
        List<Object> result = (List<Object>) OIDCAttributeMapperHelper.mapAttributeValue(model, Arrays.asList("123", "456"));
        assertEquals(Arrays.asList(123L, 456L), result);
    }

    @Test
    public void testMapAttributeValue_jsonType_invalidString() {
        ProtocolMapperModel model = createMappingModel("JSON");
        assertNull(OIDCAttributeMapperHelper.mapAttributeValue(model, "not json"));
    }

    @Test
    public void testSplitClaimPath() {
        assertThat(JsonUtils.splitClaimPath(""),          Matchers.empty());
        assertThat(JsonUtils.splitClaimPath("a"),         Matchers.contains("a"));

        assertThat(JsonUtils.splitClaimPath("a.b"),       Matchers.contains("a", "b"));
        assertThat(JsonUtils.splitClaimPath("a\\.b"),     Matchers.contains("a.b"));
        assertThat(JsonUtils.splitClaimPath("a\\\\.b"),   Matchers.contains("a\\", "b"));
        assertThat(JsonUtils.splitClaimPath("a\\\\\\.b"), Matchers.contains("a\\.b"));

        assertThat(JsonUtils.splitClaimPath("c.a\\\\.b"),   Matchers.contains("c", "a\\", "b"));
        assertThat(JsonUtils.splitClaimPath("c.a\\\\\\.b"), Matchers.contains("c", "a\\.b"));
        assertThat(JsonUtils.splitClaimPath("c\\\\\\.b.a\\\\\\.b"), Matchers.contains("c\\.b", "a\\.b"));
        assertThat(JsonUtils.splitClaimPath("c\\h\\.b.a\\\\\\.b"), Matchers.contains("ch.b", "a\\.b"));
    }
}
