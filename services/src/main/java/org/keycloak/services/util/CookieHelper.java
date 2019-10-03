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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.util.Resteasy;
import org.keycloak.common.util.ServerCookie;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieHelper {

    private static final Logger logger = Logger.getLogger(CookieHelper.class);

    /**
     * Set a response cookie.  This solely exists because JAX-RS 1.1 does not support setting HttpOnly cookies
     *
     * @param name
     * @param value
     * @param path
     * @param domain
     * @param comment
     * @param maxAge
     * @param secure
     * @param httpOnly
     */
    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly) {
        HttpResponse response = Resteasy.getContextData(HttpResponse.class);
        StringBuffer cookieBuf = new StringBuffer();
        ServerCookie.appendCookieValue(cookieBuf, 1, name, value, path, domain, comment, maxAge, secure, httpOnly);
        String cookie = cookieBuf.toString();
        response.getOutputHeaders().add(HttpHeaders.SET_COOKIE, cookie);
    }


    public static Set<String> getCookieValue(String name) {
        HttpHeaders headers = Resteasy.getContextData(HttpHeaders.class);

        Set<String> cookiesVal = new HashSet<>();

        // check for cookies in the request headers
        List<String> cookieHeader = headers.getRequestHeaders().get(HttpHeaders.COOKIE);
        if (cookieHeader != null) {
            logger.debugv("{1} cookie found in the request's header", name);
            cookieHeader.stream().map(s -> parseCookie(s, name)).forEach(cookiesVal::addAll);
        }

        // get cookies from the cookie field
        Cookie cookie = headers.getCookies().get(name);
        if (cookie != null) {
            logger.debugv("{1} cookie found in the cookie's field", name);
            cookiesVal.add(cookie.getValue());
        }


        return cookiesVal;
    }


    public static Set<String> parseCookie(String cookieHeader, String name) {
        String parts[] = cookieHeader.split("[;,]");

        Set<String> cookies = Arrays.stream(parts).filter(part -> part.startsWith(name + "=")).map(part ->
                part.substring(part.indexOf('=') + 1)).collect(Collectors.toSet());

        return cookies;
    }
}
