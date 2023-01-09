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

package org.keycloak.adapters.springsecurity.authentication;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Utility class that provides methods to create and retrieve cookies used for login redirects.
 *
 * @author <a href="mailto:scranen@gmail.com">Sjoerd Cranen</a>
 */
public final class KeycloakCookieBasedRedirect {

    private static final String REDIRECT_COOKIE = "KC_REDIRECT";

    private KeycloakCookieBasedRedirect() {}

    /**
     * Checks if a cookie with name {@value REDIRECT_COOKIE} exists, and if so, returns its value.
     * If multiple cookies of the same name exist, the value of the first cookie is returned.
     *
     * @param request the request to retrieve the cookie from.
     * @return the value of the cookie, if it exists, or else {@code null}.
     */
    public static String getRedirectUrlFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REDIRECT_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Creates a cookie with name {@value REDIRECT_COOKIE} and the given URL as value.
     * 
     * @param url the value that the cookie should have. If {@code null}, a cookie is created that
     *     expires immediately and has an empty string as value.
     * @return a cookie that can be added to a response.
     */
    public static Cookie createCookieFromRedirectUrl(String url) {
        Cookie cookie = new Cookie(REDIRECT_COOKIE, url == null ? "" : url);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        if (url == null) {
            cookie.setMaxAge(0);
        }
        return cookie;
    }
}
