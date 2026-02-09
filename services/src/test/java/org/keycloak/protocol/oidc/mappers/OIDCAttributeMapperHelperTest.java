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

import org.keycloak.utils.JsonUtils;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

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
}
