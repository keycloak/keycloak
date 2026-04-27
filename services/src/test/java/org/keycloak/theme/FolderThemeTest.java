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

import java.io.File;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import static org.keycloak.theme.Theme.Type.LOGIN;

import static org.junit.Assert.assertNotNull;

public class FolderThemeTest {

    private final Set<String> someValidISOLocaleCodes = Set.of("de", "de-DE", "de-x-informal");

    @Test
    public void testLocaleWithExtension() throws Exception {
        FolderTheme theme = new FolderTheme(new File("."), "name", LOGIN);

        for (String localeCode : someValidISOLocaleCodes) {
            Locale locale = Locale.forLanguageTag(localeCode);
            assertNotNull(theme.getMessages("base", locale));
        }
    }
}
