/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.theme;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author <a href="mailto:wadahiro@gmail.com">Hiroyuki Wada</a>
 */
public class PropertiesUtilTest {

    String valueWithUmlaut = "Umlaut: \u00E4\u00F6\u00FC";

    String key = "key";

    String propertyLine = key + "=" + valueWithUmlaut;

    @Test
    public void testEncodingIso() throws Exception {
        testWithEncoding(StandardCharsets.ISO_8859_1);
    }

    @Test
    public void testEncodingUtf8() throws Exception {
        testWithEncoding(StandardCharsets.UTF_8);
    }

    @Test
    public void testIfValueContainsSpecialCharacters() {
        assertNotEquals(valueWithUmlaut.getBytes(StandardCharsets.UTF_8), valueWithUmlaut.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void testWithEncoding(Charset charset) throws IOException {
        Properties p = new Properties();
        try (InputStream stream = new ByteArrayInputStream(propertyLine.getBytes(charset))) {
            PropertiesUtil.readCharsetAware(p, stream);
        }
        assertEquals(p.get(key), valueWithUmlaut);
    }

}
