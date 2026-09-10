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

package org.keycloak.theme;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThemeResourcesParserTest {

    @Test
    public void parseLegacyStyles() {
        Properties properties = new Properties();
        properties.setProperty("styles", "css/a.css css/b.css");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertEquals(2, resources.getStyles().size());
        assertEquals("css/a.css", resources.getStyles().get(0).getPath());
        assertEquals("css/b.css", resources.getStyles().get(1).getPath());
    }

    @Test
    public void parseDotNotationWithAttributes() {
        Properties properties = new Properties();
        properties.setProperty("styles.file1", "css/file1.css");
        properties.setProperty("styles.file1.media", "(prefers-color-scheme: light)");
        properties.setProperty("styles.file2", "css/file2.css");
        properties.setProperty("styles.file2.media", "(prefers-color-scheme: dark)");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertEquals(2, resources.getStyles().size());
        assertEquals("css/file1.css", resources.getStyles().get(0).getPath());
        assertEquals("(prefers-color-scheme: light)", resources.getStyles().get(0).getMedia());
        assertEquals("css/file2.css", resources.getStyles().get(1).getPath());
        assertEquals("(prefers-color-scheme: dark)", resources.getStyles().get(1).getMedia());
    }

    @Test
    public void parseMixesLegacyAndDotNotation() {
        Properties properties = new Properties();
        properties.setProperty("styles", "css/base.css");
        properties.setProperty("styles.light", "css/light.css");
        properties.setProperty("styles.light.media", "(prefers-color-scheme: light)");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertEquals(2, resources.getStyles().size());
        assertEquals("css/base.css", resources.getStyles().get(0).getPath());
        assertEquals("css/light.css", resources.getStyles().get(1).getPath());
        assertEquals("(prefers-color-scheme: light)", resources.getStyles().get(1).getMedia());
    }

    @Test
    public void parseExplicitOrder() {
        Properties properties = new Properties();
        properties.setProperty("styles.order", "dark,light");
        properties.setProperty("styles.light", "css/light.css");
        properties.setProperty("styles.dark", "css/dark.css");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertEquals("css/dark.css", resources.getStyles().get(0).getPath());
        assertEquals("css/light.css", resources.getStyles().get(1).getPath());
    }

    @Test
    public void parseNumericOrder() {
        Properties properties = new Properties();
        properties.setProperty("styles.2", "css/second.css");
        properties.setProperty("styles.1", "css/first.css");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertEquals("css/first.css", resources.getStyles().get(0).getPath());
        assertEquals("css/second.css", resources.getStyles().get(1).getPath());
    }

    @Test
    public void parseScriptAttributes() {
        Properties properties = new Properties();
        properties.setProperty("scripts.analytics", "js/analytics.js");
        properties.setProperty("scripts.analytics.integrity", "sha384-abc");
        properties.setProperty("scripts.analytics.defer", "true");
        properties.setProperty("scripts.analytics.type", "module");

        ThemeResourceDescriptor script = ThemeResourcesParser.parse(properties).getScripts().get(0);

        assertEquals("js/analytics.js", script.getPath());
        assertEquals("sha384-abc", script.getIntegrity());
        assertTrue(script.hasDefer());
        assertEquals("module", script.getType());
    }

    @Test
    public void parseFaviconsWithMediaAndTypeInference() {
        Properties properties = new Properties();
        properties.setProperty("favicons.svg", "favicon/favicon.svg");
        properties.setProperty("favicons.light", "favicon/favicon-light.png");
        properties.setProperty("favicons.light.media", "(prefers-color-scheme: light)");
        properties.setProperty("favicons.ico", "favicon/favicon.ico");

        List<ThemeResourceDescriptor> favicons = ThemeResourcesParser.parse(properties).getFavicons();

        assertEquals(3, favicons.size());
        assertEquals("favicon/favicon.ico", favicons.get(0).getPath());
        assertEquals("image/x-icon", favicons.get(0).getType());
        assertEquals("favicon/favicon-light.png", favicons.get(1).getPath());
        assertEquals("image/png", favicons.get(1).getType());
        assertEquals("(prefers-color-scheme: light)", favicons.get(1).getMedia());
        assertEquals("favicon/favicon.svg", favicons.get(2).getPath());
        assertEquals("image/svg+xml", favicons.get(2).getType());
        assertEquals("icon", favicons.get(2).getRel());
    }

    @Test
    public void parseLegacyFaviconProperties() {
        Properties properties = new Properties();
        properties.setProperty("favIcon", "/favicon.svg");
        properties.setProperty("favIconType", "image/svg+xml");

        ThemeResourceDescriptor favicon = ThemeResourcesParser.parse(properties).getFavicons().get(0);

        assertEquals("favicon.svg", favicon.getPath());
        assertEquals("image/svg+xml", favicon.getType());
    }

    @Test
    public void parseIgnoresUnknownAttributes() {
        Properties properties = new Properties();
        properties.setProperty("styles.main", "css/main.css");
        properties.setProperty("styles.main.unknown", "ignored");

        ThemeResourceDescriptor style = ThemeResourcesParser.parse(properties).getStyles().get(0);

        assertEquals("css/main.css", style.getPath());
        assertFalse(style.hasMedia());
    }

    @Test
    public void parseEmptyProperties() {
        ThemeResources resources = ThemeResourcesParser.parse(new Properties());

        assertTrue(resources.getStyles().isEmpty());
        assertTrue(resources.getStylesCommon().isEmpty());
        assertTrue(resources.getScripts().isEmpty());
        assertTrue(resources.getFavicons().isEmpty());
    }

    @Test
    public void parseNullProperties() {
        ThemeResources resources = ThemeResourcesParser.parse(null);

        assertTrue(resources.getStyles().isEmpty());
    }

    @Test
    public void parseSkipsBlankDotNotationValues() {
        Properties properties = new Properties();
        properties.setProperty("styles.blank", "   ");
        properties.setProperty("styles.main", "css/main.css");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertEquals(1, resources.getStyles().size());
        assertEquals("css/main.css", resources.getStyles().get(0).getPath());
    }

    @Test
    public void parseNormalizesFaviconLeadingSlash() {
        Properties properties = new Properties();
        properties.setProperty("favicons.svg", "/favicon/favicon.svg");

        ThemeResourceDescriptor favicon = ThemeResourcesParser.parse(properties).getFavicons().get(0);

        assertEquals("favicon/favicon.svg", favicon.getPath());
    }

    @Test
    public void parseNormalizesSpaceSeparatedFaviconLeadingSlash() {
        Properties properties = new Properties();
        properties.setProperty("favicons", "/favicon.svg /favicon.png");

        List<ThemeResourceDescriptor> favicons = ThemeResourcesParser.parse(properties).getFavicons();

        assertEquals(2, favicons.size());
        assertEquals("favicon.svg", favicons.get(0).getPath());
        assertEquals("favicon.png", favicons.get(1).getPath());
    }

    @Test
    public void parseIgnoresResourceIdOrder() {
        Properties properties = new Properties();
        properties.setProperty("styles.order", "css/order.css");

        ThemeResources resources = ThemeResourcesParser.parse(properties);

        assertTrue(resources.getStyles().isEmpty());
    }
}
