/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class ClaimsTest {

    @Test
    public void fromJsonString() {
        final String serializeForm = "[{\"path\": [\"given_name\"], \"mandatory\": true," +
                    "\"display\": [{\"name\": \"First Name\", \"locale\": \"en-EN\"}]}," +
                "{\"path\": [\"family_name\"], \"mandatory\": false," +
                    "\"display\": [{\"name\": \"Nachname\", \"locale\": \"de-DE\"}]}," +
                "{\"path\": [\"email\"], \"mandatory\": true," +
                    "\"display\": [{\"name\": \"E-Mail\", \"locale\": \"en-EN\"}]}]";
        Claims claims = Claims.fromJsonString(serializeForm);
        assertNotNull(claims);
        assertEquals(3, claims.size());
        assertEquals(1, claims.get(0).getPath().size());
        assertEquals("given_name", claims.get(0).getPath().get(0));
        assertTrue(claims.get(0).isMandatory());
        assertEquals(1, claims.get(0).getDisplay().size());
        assertEquals("First Name", claims.get(0).getDisplay().get(0).getName());
        assertEquals("en-EN", claims.get(0).getDisplay().get(0).getLocale());

        assertEquals(1, claims.get(1).getPath().size());
        assertEquals("family_name", claims.get(1).getPath().get(0));
        assertFalse(claims.get(1).isMandatory());
        assertEquals("Nachname", claims.get(1).getDisplay().get(0).getName());
        assertEquals("de-DE", claims.get(1).getDisplay().get(0).getLocale());

        assertEquals(1, claims.get(2).getPath().size());
        assertEquals("email", claims.get(2).getPath().get(0));
        assertTrue(claims.get(2).isMandatory());
        assertEquals("E-Mail", claims.get(2).getDisplay().get(0).getName());
        assertEquals("en-EN", claims.get(2).getDisplay().get(0).getLocale());
    }
}
