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
package org.keycloak.mdoc;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class MdocCborTest {

    @Test
    public void shouldEncodeIntegerMapAsDefiniteMinimalCbor() {
        assertArrayEquals(new byte[] { (byte) 0xA1, 0x01, 0x26 },
                MdocCbor.encodeIntegerMap(Map.of(1, -7)));
    }

    @Test
    public void shouldSortIntegerMapKeysBeforeEncoding() {
        assertArrayEquals(new byte[] { (byte) 0xA2, 0x01, 0x26, 0x18, 0x21, 0x39, 0x01, 0x00 },
                MdocCbor.encodeIntegerMap(Map.of(33, -257, 1, -7)));
    }
}
