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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import org.keycloak.common.util.HtmlUtils;
import org.owasp.html.Encoding;

/**
 * Allows sanitizing of html that uses Freemarker ?no_esc.  This way, html
 * can be allowed but it is still cleaned up for safety.  Tags and attributes
 * deemed unsafe will be stripped out.
 */
public class KeycloakSanitizerMethod implements TemplateMethodModelEx {
    
    private static final Pattern HREF_PATTERN = Pattern.compile("\\s+href=\"([^\"]*)\"");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "\\s+[A-Za-z_:][A-Za-z0-9:_.-]*\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]*))",
            Pattern.CASE_INSENSITIVE);
    
    @Override
    public Object exec(List list) throws TemplateModelException {
        if ((list.isEmpty()) || (list.get(0) == null)) {
            throw new NullPointerException("Can not escape null value.");
        }
        
        String html = list.get(0).toString();
        Map<String, String> replacements = new LinkedHashMap<>();
        if ((list.size() - 1) % 2 != 0) {
            throw new TemplateModelException("Sanitizer replacements must be marker/value pairs.");
        }
        for (int i = 1; i < list.size(); i += 2) {
            String marker = list.get(i).toString();
            String value = list.get(i + 1) == null ? "" : list.get(i + 1).toString();
            replacements.put(marker, HtmlUtils.escapeAttribute(value));
        }

        html = decodeHtmlFull(html);

        for (String marker : replacements.keySet()) {
            html = removeAttributeContaining(html, marker);
        }

        String sanitized = KeycloakSanitizerPolicy.POLICY_DEFINITION.sanitize(html);
        if (!replacements.isEmpty()) {
            StringBuilder markerPattern = new StringBuilder();
            for (String marker : replacements.keySet()) {
                if (markerPattern.length() > 0) {
                    markerPattern.append('|');
                }
                markerPattern.append(Pattern.quote(marker));
            }

            Matcher matcher = Pattern.compile(markerPattern.toString()).matcher(sanitized);
            StringBuffer result = new StringBuffer(sanitized.length());
            while (matcher.find()) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacements.get(matcher.group())));
            }
            matcher.appendTail(result);
            sanitized = result.toString();
        }
        return fixURLs(sanitized);
    }

    private String removeAttributeContaining(String html, String marker) {
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(html);
        StringBuilder result = new StringBuilder(html.length());
        int last = 0;
        while (matcher.find()) {
            String attributeValue = matcher.group(1) != null ? matcher.group(1)
                    : matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (attributeValue.contains(marker)) {
                result.append(html, last, matcher.start());
                last = matcher.end();
            }
        }
        return last == 0 ? html : result.append(html, last, html.length()).toString();
    }


    // Fully decode HTML. Assume it can be encoded multiple times
    private String decodeHtmlFull(String html) {
        if (html == null) return null;

        int MAX_DECODING_COUNT = 5; // Max count of attempts for decoding HTML (in case it was encoded multiple times)
        String decodedHtml;

        for (int i = 0; i < MAX_DECODING_COUNT; i++) {
            decodedHtml = Encoding.decodeHtml(html);
            if (decodedHtml.equals(html)) {
                // HTML is decoded. We can return it
                return html;
            } else {
                // Next attempt
                html = decodedHtml;
            }
        }

        return "";
    }

    private String fixURLs(String msg) {
        Matcher matcher = HREF_PATTERN.matcher(msg);
        if (matcher.find()) {
            int last = 0;
            StringBuilder result = new StringBuilder(msg.length());
            do {
                String href = matcher.group(1).replaceAll("&#61;", "=")
                        .replaceAll("\\.\\.", ".")
                        .replaceAll("&amp;", "&");
                result.append(msg.substring(last, matcher.start(1))).append(href);
                last = matcher.end(1);
            } while (matcher.find());
            result.append(msg.substring(last));
            return result.toString();
        }
        return msg;
    }
    
}
