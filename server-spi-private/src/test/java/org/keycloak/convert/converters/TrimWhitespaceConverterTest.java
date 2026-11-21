/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.convert.converters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.convert.ConverterConfig;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TrimWhitespaceConverterTest {

    @Test
    public void trimsLeadingAndTrailingWhitespaceByDefault() {
        assertEquals("alice", convert("  alice  "));
        assertEquals("alice", convert("alice  "));
        assertEquals("alice", convert("  alice"));
        assertEquals("alice", convert("alice"));
    }

    @Test
    public void trimsTabsAndNewlines() {
        assertEquals("alice", convert("\talice\n"));
        assertEquals("alice", convert("\n\t alice \r\n"));
    }

    @Test
    public void trimsUnicodeWhitespace() {
        // U+2003 EM SPACE is recognized as whitespace by String#strip()
        assertEquals("alice", convert("\u2003alice\u2003"));
    }

    @Test
    public void preservesInnerWhitespace() {
        assertEquals("alice smith", convert("  alice smith  "));
    }

    @Test
    public void emptyStringStaysEmpty() {
        assertEquals("", convert(""));
    }

    @Test
    public void allWhitespaceBecomesEmpty() {
        assertEquals("", convert("   "));
        assertEquals("", convert("\t\n "));
    }

    @Test
    public void nullPassesThrough() {
        assertNull(convert(null));
    }

    @Test
    public void nonStringValuesAreReturnedUnchanged() {
        Object value = 42;
        assertEquals(value, TrimWhitespaceConverter.INSTANCE.convert(value));
    }

    @Test
    public void trimsEachValueInACollection() {
        Object result = TrimWhitespaceConverter.INSTANCE.convert(Arrays.asList("  alice  ", "\tbob\n", "carol"));
        assertTrue(result instanceof List);
        assertEquals(Arrays.asList("alice", "bob", "carol"), result);
    }

    @Test
    public void emptyCollectionReturnsEmptyCollection() {
        Object result = TrimWhitespaceConverter.INSTANCE.convert(Collections.emptyList());
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void trimOnlyLeadingWhenTrailingDisabled() {
        ConverterConfig config = ConverterConfig.builder()
                .config(TrimWhitespaceConverter.KEY_TRIM_TRAILING, false)
                .build();

        assertEquals("alice  ", TrimWhitespaceConverter.INSTANCE.convert("  alice  ", config));
    }

    @Test
    public void trimOnlyTrailingWhenLeadingDisabled() {
        ConverterConfig config = ConverterConfig.builder()
                .config(TrimWhitespaceConverter.KEY_TRIM_LEADING, false)
                .build();

        assertEquals("  alice", TrimWhitespaceConverter.INSTANCE.convert("  alice  ", config));
    }

    @Test
    public void noopWhenBothDisabled() {
        ConverterConfig config = ConverterConfig.builder()
                .config(TrimWhitespaceConverter.KEY_TRIM_LEADING, false)
                .config(TrimWhitespaceConverter.KEY_TRIM_TRAILING, false)
                .build();

        assertEquals("  alice  ", TrimWhitespaceConverter.INSTANCE.convert("  alice  ", config));
    }

    @Test
    public void stringBooleanConfigValuesAreAccepted() {
        ConverterConfig config = ConverterConfig.builder()
                .config(TrimWhitespaceConverter.KEY_TRIM_LEADING, "false")
                .build();

        assertEquals("  alice", TrimWhitespaceConverter.INSTANCE.convert("  alice  ", config));
    }

    private Object convert(Object input) {
        return TrimWhitespaceConverter.INSTANCE.convert(input);
    }
}
