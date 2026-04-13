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

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.utils.JsonUtils;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 *
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

    @Test
    public void testMapAttributeValueWithEmptyStringReturnsOriginalValueForLong() {
        ProtocolMapperModel model = createMapperModel("long");
        // When conversion fails, returns the original value instead of null
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, ""), is(""));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "   "), is("   "));
    }

    @Test
    public void testMapAttributeValueWithEmptyStringReturnsOriginalValueForInteger() {
        ProtocolMapperModel model = createMapperModel("int");
        // When conversion fails, returns the original value instead of null
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, ""), is(""));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "   "), is("   "));
    }

    @Test
    public void testMapAttributeValueWithEmptyStringReturnsOriginalValueForBoolean() {
        ProtocolMapperModel model = createMapperModel("boolean");
        // When conversion fails, returns the original value instead of null
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, ""), is(""));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "   "), is("   "));
    }

    @Test
    public void testMapAttributeValueWithInvalidStringReturnsOriginalValueForLong() {
        ProtocolMapperModel model = createMapperModel("long");
        // When conversion fails, returns the original value instead of null
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "abc"), is("abc"));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "12.34"), is("12.34"));
    }

    @Test
    public void testMapAttributeValueWithInvalidStringReturnsOriginalValueForInteger() {
        ProtocolMapperModel model = createMapperModel("int");
        // When conversion fails, returns the original value instead of null
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "abc"), is("abc"));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "12.34"), is("12.34"));
    }

    @Test
    public void testMapAttributeValueWithValidStringReturnsCorrectValueForLong() {
        ProtocolMapperModel model = createMapperModel("long");
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "123"), is(123L));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "-456"), is(-456L));
    }

    @Test
    public void testMapAttributeValueWithValidStringReturnsCorrectValueForInteger() {
        ProtocolMapperModel model = createMapperModel("int");
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "123"), is(123));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "-456"), is(-456));
    }

    @Test
    public void testMapAttributeValueWithValidStringReturnsCorrectValueForBoolean() {
        ProtocolMapperModel model = createMapperModel("boolean");
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "true"), is(true));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "false"), is(false));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "TRUE"), is(true));
        assertThat(OIDCAttributeMapperHelper.mapAttributeValue(model, "FALSE"), is(false));
    }

    private ProtocolMapperModel createMapperModel(String jsonType) {
        return OIDCAttributeMapperHelper.createClaimMapper(
            "testMapper",
            "userAttr",
            "testClaim",
            jsonType,
            true,
            true,
            true,
            false,
            null
        );
    }
}
