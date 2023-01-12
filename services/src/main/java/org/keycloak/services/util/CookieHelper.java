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

import org.jboss.logging.Logger;
import org.jboss.resteasy.util.CookieParser;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CookieHelper {

    private static final Logger logger = Logger.getLogger(CookieHelper.class);

    public static void addCookie(KeycloakSession session, NewCookie cookie){
        session.getContext().getHttpResponse().addCookie(cookie);
    }

    public static Set<String> getCookieValue(HttpHeaders headers, String name) {
        // check for cookies in the request headers
        Set<String> cookiesVal = new HashSet<>(parseCookie(headers.getRequestHeaders().getFirst(HttpHeaders.COOKIE), name));

        // get cookies from the cookie field
        Cookie cookie = headers.getCookies().get(name);
        if (cookie != null) {
            logger.debugv("{0} cookie found in the cookie field", name);
            cookiesVal.add(cookie.getValue());
        }

        return cookiesVal;
    }


    public static Set<String> parseCookie(String header, String name) {
        if (header == null || name == null) {
            return Collections.emptySet();
        }

        Set<String> values = new HashSet<>();

        for (Cookie cookie : CookieParser.parseCookies(header)) {
            if (name.equals(cookie.getName())) {
                logger.debugv("{0} cookie found in the request header", name);
                values.add(cookie.getValue());
            }
        }

        return values;
    }

    public static Optional<Cookie> getCookie(Map<String, Cookie> cookies, String name) {
        return Optional.ofNullable(cookies.get(name));
    }

    public static void expireCookie(HttpResponse response, String name, String path, boolean secure, boolean httpOnly) {
        logger.debugf("Expiring cookie: %s path: %s", name, path);

        final NewCookie cookie = new CookieBuilder(name, "")
                .path(path)
                .comment("Expiring cookie")
                .maxAge(0)
                .secure(secure)
                .httpOnly(httpOnly)
                .build();

        response.addCookie(cookie);
    }
}
