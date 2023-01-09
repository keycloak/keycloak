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

package org.keycloak.adapters.springsecurity.filter;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.keycloak.constants.AdapterConstants;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Matches a request if it contains a {@value AdapterConstants#KEYCLOAK_ADAPTER_STATE_COOKIE}
 * cookie.
 *
 * @author <a href="mailto:scranen@gmail.com">Sjoerd Cranen</a>
 */
public class AdapterStateCookieRequestMatcher implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return false;
        }
        for (Cookie cookie: request.getCookies()) {
            if (AdapterConstants.KEYCLOAK_ADAPTER_STATE_COOKIE.equals(cookie.getName())) {
                return true;
            }
        }
        return false;
    }
}
