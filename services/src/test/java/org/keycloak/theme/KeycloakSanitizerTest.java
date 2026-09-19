/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the KeycloakEscape utility.
 * 
 * @author Stan Silvert
 */
public class KeycloakSanitizerTest {
    private final KeycloakSanitizerMethod kcEscape = new KeycloakSanitizerMethod();
    
    @Test
    public void testEscapes() throws Exception {
        List<String> html = new ArrayList<>();

        html.add("<div class=\"kc-logo-text\"><script>alert('foo');</script><span>Keycloak</span></div>");
        String expectedResult = "<div class=\"kc-logo-text\"><span>Keycloak</span></div>";
        assertResult(expectedResult, html);
        
        html.set(0, "<h1>Foo</h1>");
        expectedResult = "<h1>Foo</h1>";
        assertResult(expectedResult, html);
        
        html.set(0, "<div class=\"kc-logo-text\"><span>Keycloak</span></div><svg onload=alert(document.cookie);>");
        expectedResult = "<div class=\"kc-logo-text\"><span>Keycloak</span></div>";
        assertResult(expectedResult, html);
        
        html.set(0, null);
        expectedResult = null;
        try {
            assertResult(expectedResult, html);
            fail("Expected NPE");
        } catch (NullPointerException npe) {}
        
        html.set(0, "");
        expectedResult = "";
        assertResult(expectedResult, html);
    }

    @Test
    public void testLinks() throws Exception {
        List<String> html = new ArrayList<>();

        html.add("<a href=\"https://www.example.org/sub-page\">Link text</a>");
        String expectedResult = "<a href=\"https://www.example.org/sub-page\" rel=\"nofollow\">Link text</a>";
        assertResult(expectedResult, html);

        html.set(0, "<a href=\"https://www.example.org/terms-of-service\" target=\"_blank\">Link text</a>");
        expectedResult = "<a href=\"https://www.example.org/terms-of-service\" target=\"_blank\" rel=\"nofollow noopener noreferrer\">Link text</a>";
        assertResult(expectedResult, html);

        html.set(0, "<a href=\"https://www.example.org/sub-page\" target=\"_top\">Link text</a>");
        expectedResult = "<a href=\"https://www.example.org/sub-page\" rel=\"nofollow\">Link text</a>";
        assertResult(expectedResult, html);

        html.set(0, "<a href=\"https://www.example.org/sub-page\" target=\"someframe\">Link text</a>");
        expectedResult = "<a href=\"https://www.example.org/sub-page\" rel=\"nofollow\">Link text</a>";
        assertResult(expectedResult, html);
    }

    @Test
    public void testUrls() throws Exception {
        List<String> html = new ArrayList<>();

        html.add("<p><a href='https://localhost'>link</a></p>");
        assertResult("<p><a href=\"https://localhost\" rel=\"nofollow\">link</a></p>", html);

        html.set(0, "<p><a href=\"\">link</a></p>");
        assertResult("<p>link</p>", html);

        html.set(0, "<p><a href=\"javascript:alert('hello!');\">link</a></p>");
        assertResult("<p>link</p>", html);

        html.set(0, "<p><a href=\"javascript:alert(document.domain);\">link</a></p>");
        assertResult("<p>link</p>", html);

        html.set(0, "<p><a href=\"javascript&colon;alert(document.domain);\">link</a></p>");
        assertResult("<p>link</p>", html);

        // Effectively same as previous case, but with \0 character added
        html.set(0, "<p><a href=\"javascript&\0colon;alert(document.domain);\">link</a></p>");
        assertResult("<p>link</p>", html);

        html.set(0, "<p><a href=\"javascript&amp;amp;\0colon;alert(document.domain);\">link</a></p>");
        assertResult("<p>link</p>", html);

        html.set(0, "<p><a href=\"javascript&amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;amp;\0colon;alert(document.domain);\">link</a></p>");
        assertResult("", html);

        html.set(0, "<p><a href=\"https://localhost?key=123&msg=abc\">link</a></p>");
        assertResult("<p><a href=\"https://localhost?key=123&msg=abc\" rel=\"nofollow\">link</a></p>", html);

        html.set(0, "<p><a href='https://localhost?key=123&msg=abc'>link1</a><a href=\"https://localhost?key=abc&msg=123\">link2</a></p>");
        assertResult("<p><a href=\"https://localhost?key=123&msg=abc\" rel=\"nofollow\">link1</a><a href=\"https://localhost?key=abc&msg=123\" rel=\"nofollow\">link2</a></p>", html);
    }

    @Test
    public void testRemovesSentinelUrlAttributes() throws Exception {
        List<String> html = new ArrayList<>();

        html.add("<a href=\"__KC_SENTINEL0_123__\">link</a>");
        html.add("__KC_SENTINEL0_123__");
        html.add("javascript:alert(1)");
        assertResult("<a rel=\"nofollow\">link</a>", html);

        html.clear();
        html.add("<img src=\"https://example.org/__KC_SENTINEL1_123__\">");
        html.add("__KC_SENTINEL1_123__");
        html.add("javascript:alert(1)");
        assertResult("<img />", html);

        html.clear();
        html.add("<p style=\"font-family: __KC_SENTINEL0_123__\">text</p>");
        html.add("__KC_SENTINEL0_123__");
        html.add("url(https://evil.example)");
        assertResult("<p>text</p>", html);

        html.clear();
        html.add("<a/href=\"__KC_SENTINEL0_123__\">link</a>");
        html.add("__KC_SENTINEL0_123__");
        html.add("javascript:alert(1)");
        assertResult("<a rel=\"nofollow\">link</a>", html);
    }

    @Test
    public void testSanitizeWithoutReplacementsDoesNotFilterSentinelText() throws Exception {
        assertResult("<p>__KC_SENTINEL0_123__</p>",
                new ArrayList<>(List.of("<p>__KC_SENTINEL0_123__</p>")));

        assertResult("<p>label=\"safe\"</p>",
                new ArrayList<>(List.of("<p>label=\"__KC_SENTINEL0_123__\"</p>",
                        "__KC_SENTINEL0_123__", "safe")));
    }

    @Test
    public void testSanitizeTextPreservesDetailsWithoutMarkup() {
        assertEquals("R&D Team", KeycloakSanitizerPolicy.sanitizeText("R&D Team"));
        assertEquals("Click", KeycloakSanitizerPolicy.sanitizeText("<a href=\"https://evil.example\">Click</a>"));
        assertEquals("",
                KeycloakSanitizerPolicy.sanitizeText("&lt;img src=x onerror=alert(1)&gt;"));
        assertEquals("Click",
                KeycloakSanitizerPolicy.sanitizeText("&amp;lt;a href=\"https://evil.example\"&amp;gt;Click&amp;lt;/a&amp;gt;"));
    }

    private void assertResult(String expectedResult, List<String> html) throws Exception {
        String result = kcEscape.exec(html).toString();
        assertEquals(expectedResult, result);
    }

}
