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

package org.keycloak.saml.processing.core.saml.v2.util;

import java.util.Date;

import org.keycloak.saml.common.ErrorCodes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class XMLTimeUtilTest {

    @Test
    public void testParse() {
        assertEquals("2009-06-17T18:55:10.738Z", XMLTimeUtil.parse("2009-06-17T18:55:10.738Z").toXMLFormat());
        assertEquals("2002-05-30T09:30:10-06:00", XMLTimeUtil.parse("2002-05-30T09:30:10-06:00").toXMLFormat());
    }

    @Test
    public void testParseNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parse(null));
        assertEquals(ErrorCodes.NULL_ARGUMENT + "timeString", e.getMessage());
    }

    @Test
    public void testParseInvalid() {
        assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parse("not-a-date"));
    }

    @Test
    public void testParseAsDuration() {
        Date start = new Date(0);
        assertEquals(600000L, XMLTimeUtil.parseAsDuration("PT10M").getTimeInMillis(start));
        assertEquals(1000L, XMLTimeUtil.parseAsDuration("1000").getTimeInMillis(start));
    }

    @Test
    public void testParseAsDurationNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parseAsDuration(null));
        assertEquals(ErrorCodes.NULL_ARGUMENT + "duration time", e.getMessage());
    }

    @Test
    public void testParseAsDurationInvalid() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parseAsDuration("ten minutes"));
        assertTrue(e.getMessage().startsWith("Cache duration could not be created using 'ten minutes'"));
    }
}
