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

package org.keycloak.services.util;

import jakarta.ws.rs.core.Cookie;
import org.keycloak.http.HttpCookie;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

import static org.keycloak.common.util.ServerCookie.SameSiteAttributeValue;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieHelper {

    public static final String LEGACY_COOKIE = "_LEGACY";

    /**
     * Set a response cookie.  This solely exists because JAX-RS 1.1 does not support setting HttpOnly cookies
     * @param name
     * @param value
     * @param path
     * @param domain
     * @param comment
     * @param maxAge
     * @param secure
     * @param httpOnly
     * @param sameSite
     */
    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly, SameSiteAttributeValue sameSite, KeycloakSession session) {
        SameSiteAttributeValue sameSiteParam = sameSite;
        // when expiring a cookie we shouldn't set the sameSite attribute; if we set e.g. SameSite=None when expiring a cookie, the new cookie (with maxAge == 0)
        // might be rejected by the browser in some cases resulting in leaving the original cookie untouched; that can even prevent user from accessing their application
        if (maxAge == 0) {
            sameSite = null;
        }

        boolean secure_sameSite = sameSite == SameSiteAttributeValue.NONE || secure; // when SameSite=None, Secure attribute must be set

        HttpResponse response = session.getContext().getHttpResponse();
        HttpCookie cookie = new HttpCookie(1, name, value, path, domain, comment, maxAge, secure_sameSite, httpOnly, sameSite);

        response.setCookieIfAbsent(cookie);

        // a workaround for browser in older Apple OSs – browsers ignore cookies with SameSite=None
        if (sameSiteParam == SameSiteAttributeValue.NONE) {
            addCookie(name + LEGACY_COOKIE, value, path, domain, comment, maxAge, secure, httpOnly, null, session);
        }
    }

    /**
     * Set a response cookie avoiding SameSite parameter
     * @param name
     * @param value
     * @param path
     * @param domain
     * @param comment
     * @param maxAge
     * @param secure
     * @param httpOnly
     */
    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly, KeycloakSession session) {
        addCookie(name, value, path, domain, comment, maxAge, secure, httpOnly, null, session);
    }

    public static String getCookieValue(KeycloakSession session, String name) {
        Map<String, Cookie> cookies = session.getContext().getRequestHeaders().getCookies();
        Cookie cookie = cookies.get(name);
        if (cookie == null) {
            String legacy = name + LEGACY_COOKIE;
            cookie = cookies.get(legacy);
        }
        return cookie != null ? cookie.getValue() : null;
    }

}
