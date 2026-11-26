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
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.saml.common.ErrorCodes;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class XMLTimeUtilTest {

    @Test
    public void parse() {
        assertEquals("2009-06-17T18:55:10.738Z", XMLTimeUtil.parse("2009-06-17T18:55:10.738Z").toXMLFormat());
        assertEquals("2002-05-30T09:30:10-06:00", XMLTimeUtil.parse("2002-05-30T09:30:10-06:00").toXMLFormat());
    }

    @Test
    public void parseNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parse(null));
        assertEquals(ErrorCodes.NULL_ARGUMENT + "timeString", e.getMessage());
    }

    @Test
    public void parseInvalid() {
        assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parse("not-a-date"));
    }

    @Test
    public void parseAsDuration() {
        Date start = new Date(0);
        assertEquals(600000L, XMLTimeUtil.parseAsDuration("PT10M").getTimeInMillis(start));
        assertEquals(1000L, XMLTimeUtil.parseAsDuration("1000").getTimeInMillis(start));
    }

    @Test
    public void parseAsDurationNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parseAsDuration(null));
        assertEquals(ErrorCodes.NULL_ARGUMENT + "duration time", e.getMessage());
    }

    @Test
    public void parseAsDurationInvalid() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> XMLTimeUtil.parseAsDuration("ten minutes"));
        assertTrue(e.getMessage().startsWith("Cache duration could not be created"));
    }

    @Test
    public void parseValidDateTime() {
        XMLGregorianCalendar cal = XMLTimeUtil.parse("2024-01-15T10:30:00.123Z");
        assertNotNull(cal);
        assertEquals(2024, cal.getYear());
        assertEquals(1, cal.getMonth());
        assertEquals(15, cal.getDay());
    }

    @Test
    public void parseValidDateTimeWithTimezone() {
        XMLGregorianCalendar cal = XMLTimeUtil.parse("2024-06-01T23:59:59.999+14:00");
        assertNotNull(cal);
    }

    @Test
    public void parseRejectsOversizedDateTime() {
        String malicious = "2024-01-01T00:00:00." + "1".repeat(1_000) + "Z";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> XMLTimeUtil.parse(malicious));
        assertThat(ex.getMessage(), containsString("exceeds the maximum allowed"));
        assertThat(ex.getMessage(), containsString("dateTime"));
    }

    @Test
    public void parseValidDuration() {
        Duration d = XMLTimeUtil.parseAsDuration("P1Y2M3DT4H5M6S");
        assertNotNull(d);
        assertEquals(1, d.getYears());
    }

    @Test
    public void parseValidDurationNumeric() {
        Duration d = XMLTimeUtil.parseAsDuration("3600000");
        assertNotNull(d);
    }

    @Test
    public void parseAsDurationRejectsOversizedDuration() {
        String malicious = "P" + "1".repeat(1_000) + "Y";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> XMLTimeUtil.parseAsDuration(malicious));
        assertThat(ex.getMessage(), containsString("exceeds the maximum allowed"));
        assertThat(ex.getMessage(), containsString("duration"));
    }

    @Test
    public void parseAcceptsMaxLengthDateTime() {
        // 100 chars is at the boundary — must still be accepted (even if not a valid date, the length check passes)
        StringBuilder sb = new StringBuilder("2024-01-01T00:00:00.");
        while (sb.length() < XMLTimeUtil.MAX_TIME_STRING_LENGTH - 1) {
            sb.append('1');
        }
        sb.append('Z');
        Assert.assertTrue("Test string should be at most MAX_TIME_STRING_LENGTH",
                sb.length() <= XMLTimeUtil.MAX_TIME_STRING_LENGTH);
        // Should not throw IllegalArgumentException for length — may throw for invalid format
        try {
            XMLTimeUtil.parse(sb.toString());
        } catch (IllegalArgumentException e) {
            // Length-based rejection must NOT happen here
            if (e.getMessage().contains("exceeds the maximum allowed")) {
                Assert.fail("Should not reject a dateTime value within the length limit");
            }
        }
    }

    @Test
    public void parseRejectsJustOverMaxLengthDateTime() {
        StringBuilder sb = new StringBuilder("2024-01-01T00:00:00.");
        while (sb.length() <= XMLTimeUtil.MAX_TIME_STRING_LENGTH) {
            sb.append('1');
        }
        sb.append('Z');
        Assert.assertTrue("Test string should exceed MAX_TIME_STRING_LENGTH",
                sb.length() > XMLTimeUtil.MAX_TIME_STRING_LENGTH);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> XMLTimeUtil.parse(sb.toString()));
        assertThat(ex.getMessage(), containsString("exceeds the maximum allowed"));
    }

}
