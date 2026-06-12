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
package org.keycloak.tests.utils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helpers to assert that inline scripts rendered by the server carry a CSP nonce.
 */
public final class InlineScriptNonceUtil {

    private static final Pattern SCRIPT_OPEN_TAG = Pattern.compile("<script\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern NONCE_ATTRIBUTE = Pattern.compile("\\snonce\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE);
    private static final Pattern TYPE_ATTRIBUTE = Pattern.compile("\\stype\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern SRC_ATTRIBUTE = Pattern.compile("\\ssrc\\s*=", Pattern.CASE_INSENSITIVE);

    private InlineScriptNonceUtil() {
    }

    /**
     * @return opening tags of executable inline scripts (no {@code src}) that miss a {@code nonce} attribute
     */
    public static List<String> getInlineScriptTagsWithoutNonce(String html) {
        return SCRIPT_OPEN_TAG.matcher(html).results()
                .map(MatchResult::group)
                .filter(InlineScriptNonceUtil::isInlineScript)
                .filter(InlineScriptNonceUtil::isExecutableScript)
                .filter(group -> getNonce(group) == null)
                .toList();
    }

    /**
     * @return values of all {@code nonce} attributes on executable script tags
     */
    public static Set<String> getScriptNonceValues(String html) {
        return SCRIPT_OPEN_TAG.matcher(html).results()
                .map(MatchResult::group)
                .filter(InlineScriptNonceUtil::isExecutableScript)
                .map(InlineScriptNonceUtil::getNonce)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean isInlineScript(String openTag) {
        return !SRC_ATTRIBUTE.matcher(openTag).find();
    }

    /**
     * Data blocks such as {@code type="application/json"} are not executed and therefore not subject to the
     * script-src CSP directive.
     */
    private static boolean isExecutableScript(String openTag) {
        Matcher type = TYPE_ATTRIBUTE.matcher(openTag);
        if (type.find()) {
            String value = type.group(1).toLowerCase();
            return !value.contains("json") && !value.contains("template");
        }
        return true;
    }

    private static String getNonce(String openTag) {
        Matcher nonce = NONCE_ATTRIBUTE.matcher(openTag);
        if (nonce.find() && !nonce.group(2).isBlank()) {
            return nonce.group(2);
        }
        return null;
    }
}
