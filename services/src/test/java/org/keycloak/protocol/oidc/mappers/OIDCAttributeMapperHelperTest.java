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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.utils.JsonUtils;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author hmlnarik
 */
public class OIDCAttributeMapperHelperTest {

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

    // --- mapAttributeValue: long type ---

    @Test
    public void mapAttributeValue_longType_validValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), Collections.singletonList("12345"));
        assertEquals(12345L, result);
    }

    @Test
    public void mapAttributeValue_longType_emptyString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), Collections.singletonList(""));
        assertNull(result);
    }

    @Test
    public void mapAttributeValue_longType_blankString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), Collections.singletonList("   "));
        assertNull(result);
    }

    @Test
    public void mapAttributeValue_longType_invalidString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), Collections.singletonList("not-a-number"));
        assertNull(result);
    }

    @Test
    public void mapAttributeValue_longType_negativeValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), Collections.singletonList("-99"));
        assertEquals(-99L, result);
    }

    // --- mapAttributeValue: int type ---

    @Test
    public void mapAttributeValue_intType_validValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("int"), Collections.singletonList("42"));
        assertEquals(42, result);
    }

    @Test
    public void mapAttributeValue_intType_emptyString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("int"), Collections.singletonList(""));
        assertNull(result);
    }

    @Test
    public void mapAttributeValue_intType_invalidString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("int"), Collections.singletonList("abc"));
        assertNull(result);
    }

    // --- mapAttributeValue: boolean type ---

    @Test
    public void mapAttributeValue_booleanType_trueValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("boolean"), Collections.singletonList("true"));
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void mapAttributeValue_booleanType_falseValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("boolean"), Collections.singletonList("false"));
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void mapAttributeValue_booleanType_emptyString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("boolean"), Collections.singletonList(""));
        assertNull(result);
    }

    // --- mapAttributeValue: String type ---

    @Test
    public void mapAttributeValue_stringType_value() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("String"), Collections.singletonList("hello"));
        assertEquals("hello", result);
    }

    @Test
    public void mapAttributeValue_stringType_emptyString() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("String"), Collections.singletonList(""));
        assertEquals("", result);
    }

    // --- mapAttributeValue: null and empty collection ---

    @Test
    public void mapAttributeValue_nullValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), null);
        assertNull(result);
    }

    @Test
    public void mapAttributeValue_emptyCollection() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("long"), Collections.emptyList());
        assertNull(result);
    }

    // --- mapAttributeValue: no JSON type configured ---

    @Test
    public void mapAttributeValue_noType_returnsRawValue() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel(null), Collections.singletonList("raw-value"));
        assertEquals("raw-value", result);
    }

    // --- mapAttributeValue: JSON type with invalid value ---

    @Test
    public void mapAttributeValue_jsonType_invalidJson() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("JSON"), Collections.singletonList("not-json"));
        assertNull(result);
    }

    @Test
    public void mapAttributeValue_jsonType_validJson() {
        Object result = OIDCAttributeMapperHelper.mapAttributeValue(
                createMappingModel("JSON"), Collections.singletonList("{\"key\":\"value\"}"));
        assertTrue(result instanceof com.fasterxml.jackson.databind.JsonNode);
    }

    private static ProtocolMapperModel createMappingModel(String jsonType) {
        ProtocolMapperModel model = new ProtocolMapperModel();
        model.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        model.setName("test-mapper");
        Map<String, String> config = new HashMap<>();
        if (jsonType != null) {
            config.put(OIDCAttributeMapperHelper.JSON_TYPE, jsonType);
        }
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "test.claim");
        model.setConfig(config);
        return model;
    }
}
