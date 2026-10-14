/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.ipatuura_user_spi;

import org.junit.Assert;
import org.junit.Test;

public class IpatuuraTest {

    @Test
    public void testBuildFilterKeepsPlainValue() {
        Assert.assertEquals("userName eq \"jdoe\"", Ipatuura.buildFilter("userName", "jdoe"));
        Assert.assertEquals("emails.value eq \"jdoe@example.com\"", Ipatuura.buildFilter("emails.value", "jdoe@example.com"));
    }

    @Test
    public void testBuildFilterEscapesQuote() {
        // the value must stay a single string literal, so the quote is escaped
        Assert.assertEquals("userName eq \"a\\\"b\"", Ipatuura.buildFilter("userName", "a\"b"));
    }

    @Test
    public void testBuildFilterEscapesBackslash() {
        Assert.assertEquals("userName eq \"a\\\\b\"", Ipatuura.buildFilter("userName", "a\\b"));
        // a trailing backslash must not swallow the closing quote
        Assert.assertEquals("userName eq \"a\\\\\"", Ipatuura.buildFilter("userName", "a\\"));
    }
}
