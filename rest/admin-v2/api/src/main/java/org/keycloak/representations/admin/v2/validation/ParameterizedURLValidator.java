/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.admin.v2.validation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Deque;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates URLs that may contain parameterized placeholders like {@code {param}}.
 * <p>
 * This validator URL-encodes curly brackets before validation, allowing parameterized
 * URLs like {@code http://{host}/callback} to pass validation.
 */
public class ParameterizedURLValidator implements ConstraintValidator<ParameterizedURL, String> {

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        if (url == null || url.isEmpty()) {
            return true;
        }

        if (!checkCurlyBracketsBalanced(url)) {
            return false;
        }

        String urlToCheck = url.replace("{", "%7B").replace("}", "%7D");

        try {
            var uri = new URI(urlToCheck).toURL();
            if (uri.getProtocol() != null && uri.getProtocol().length() > 0) {
              return false;
            }

            if (uri.getHost() != null && uri.getHost().length() > 0) {
              return false;
            }

            if (uri.getPort() != -1) {
              return false;
            }
            return true;
          } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
          }
        }

    /**
     * Check if url has curly brackets in correct position ('{' before '}').
     *
     * @param url the URL to check
     * @return true if curly brackets are balanced, false otherwise
     */
    static boolean checkCurlyBracketsBalanced(String url) {
        Deque<Character> stack = new ArrayDeque<>();

        for (char c : url.toCharArray()) {
            if (c == '{') {
                stack.push(c);
                continue;
            }
            if (stack.isEmpty() && c == '}') {
                return false;
            }
            if (c == '}') {
                char check = stack.pop();
                if (check != '{') {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }
}
