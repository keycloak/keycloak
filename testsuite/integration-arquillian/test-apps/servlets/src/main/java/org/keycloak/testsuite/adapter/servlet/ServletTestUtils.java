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

package org.keycloak.testsuite.adapter.servlet;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.common.util.UriUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletTestUtils {

    // TODO: Couldn't just always read urlBase from req.getRequestURI() ?
    public static String getUrlBase(HttpServletRequest req) {
        if (System.getProperty("app.server.ssl.required", "false").equals("true")) {
            return System.getProperty("app.server.ssl.base.url", "https://localhost:8643");
        }

        String urlBase = System.getProperty("app.server.base.url");

        if (urlBase == null) {
            String authServer = System.getProperty("auth.server.container", "auth-server-undertow");
            if (authServer.contains("undertow")) {
                urlBase = UriUtils.getOrigin(req.getRequestURL().toString());
            } else {
                urlBase = "http://localhost:8280";
            }
        }

        return urlBase;
    }

    public static String getAuthServerUrlBase() {
        if (System.getProperty("auth.server.ssl.required", "false").equals("true")) {
            return System.getProperty("auth.server.ssl.base.url", "https://localhost:8543");
        }

        return System.getProperty("auth.server.base.url", "http://localhost:8180");
    }
}
