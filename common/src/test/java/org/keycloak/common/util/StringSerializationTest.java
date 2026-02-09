/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.common.util;

import java.net.URI;

import org.keycloak.common.util.StringSerialization.Deserializer;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 * @author hmlnarik
 */
public class StringSerializationTest {

    @Test
    public void testString() {
        String a = "aa";
        String b = "a:\na";
        String c = null;
        String d = "a::a";
        String e = "::";

        String serialized = StringSerialization.serialize(a, b, c, d, e);
        Deserializer deserializer = StringSerialization.deserialize(serialized);

        assertThat(deserializer.next(String.class), is(a));
        assertThat(deserializer.next(String.class), is(b));
        assertThat(deserializer.next(String.class), is(c));
        assertThat(deserializer.next(String.class), is(d));
        assertThat(deserializer.next(String.class), is(e));
        assertThat(deserializer.next(String.class), nullValue());
    }

    @Test
    public void testStringWithSeparators() {
        String a = ";;";
        String b = "a;a";
        String c = null;
        String d = "a;;a";
        String e = ";;";

        String serialized = StringSerialization.serialize(a, b, c, d, e);
        Deserializer deserializer = StringSerialization.deserialize(serialized);

        assertThat(deserializer.next(String.class), is(a));
        assertThat(deserializer.next(String.class), is(b));
        assertThat(deserializer.next(String.class), is(c));
        assertThat(deserializer.next(String.class), is(d));
        assertThat(deserializer.next(String.class), is(e));
        assertThat(deserializer.next(String.class), nullValue());
    }

    @Test
    public void testStringUri() {
        String a = ";;";
        String b = "a;a";
        String c = null;
        URI d = URI.create("http://my.domain.com");
        String e = ";;";

        String serialized = StringSerialization.serialize(a, b, c, d, e);
        Deserializer deserializer = StringSerialization.deserialize(serialized);

        assertThat(deserializer.next(String.class), is(a));
        assertThat(deserializer.next(String.class), is(b));
        assertThat(deserializer.next(String.class), is(c));
        assertThat(deserializer.next(URI.class), is(d));
        assertThat(deserializer.next(String.class), is(e));
        assertThat(deserializer.next(String.class), nullValue());
    }

}
