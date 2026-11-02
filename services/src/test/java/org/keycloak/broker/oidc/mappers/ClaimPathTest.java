/*
 * Copyright 2026 Andrzej Świętek
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

package org.keycloak.broker.oidc.mappers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ClaimPathTest {

    private static final JsonNode CLAIMS = claims("{\"iss\":\"https://idp\",\"org\":{\"name\":\"acme\",\"list\":[\" a \",[\"b\"]]},"
            + "\"groups\":[\"g0\",\"g1\"],\"a\":[[\"x\",\"y\",\"z\"],[\"p\",\"q\",\"r\"]],\"dotted.name\":\"c\",\"nul\":null}");

    private static JsonNode claims(String json) {
        try {
            return JsonSerialization.mapper.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Optional<String> text(String claim) {
        return ClaimPath.find(CLAIMS, claim).map(JsonNode::textValue);
    }

    private static Object upstream(String claim) {
        return JsonUtils.getJsonValue(CLAIMS, claim);
    }

    @Test
    public void nestedAndIndexedValuesAreReadAsSent() {
        assertThat(text("iss"), equalTo(Optional.of("https://idp")));
        assertThat(text("org.list[0]"), equalTo(Optional.of(" a ")));
        assertThat(ClaimPath.find(CLAIMS, "org.list[1]").map(JsonNode::isArray), equalTo(Optional.of(true)));
        assertThat(text("dotted\\.name"), equalTo(Optional.of("c")));
    }

    @Test
    public void missingOrInvalidPathsFindNothing() {
        assertThat(ClaimPath.find(CLAIMS, "org.list[2]"), equalTo(Optional.empty()));
        assertThat(ClaimPath.find(CLAIMS, "org.list[x]"), equalTo(Optional.empty()));
        assertThat(ClaimPath.find(CLAIMS, "org.list[-1]"), equalTo(Optional.empty()));
        assertThat(ClaimPath.find(CLAIMS, "nul"), equalTo(Optional.empty()));
        assertThat(ClaimPath.find(CLAIMS, "org."), equalTo(Optional.empty()));
    }

    @Test
    public void pathsAreReadAsJsonUtilsReadsThem() {
        assertThat(text("groups [0]"), equalTo(Optional.of("g0")));
        assertThat(upstream("groups [0]"), equalTo("g0"));

        assertThat(text("groups[ 1 ]"), equalTo(Optional.of("g1")));
        assertThat(upstream("groups[ 1 ]"), equalTo("g1"));

        assertThat(ClaimPath.find(CLAIMS, "a[1][2]"), equalTo(Optional.empty()));
        assertThat(upstream("a[1][2]"), nullValue());

        assertThat(text("org[0].name"), equalTo(Optional.of("acme")));
        assertThat(upstream("org[0].name"), equalTo("acme"));

        assertThat(text("iss.host"), equalTo(Optional.of("https://idp")));
        assertThat(upstream("iss.host"), equalTo("https://idp"));

        assertThat(text("iss.host[x]"), equalTo(Optional.of("https://idp")));
        assertThat(upstream("iss.host[x]"), equalTo("https://idp"));

        assertThat(ClaimPath.find(CLAIMS, "groups.first").map(JsonNode::size), equalTo(Optional.of(2)));
        assertThat(upstream("groups.first"), equalTo(List.of("g0", "g1")));
    }
}
