/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.testsuite.servletauthz;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebAppHelper {

    public static String getAuthUri() {
        boolean isTLSEnabled = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));
        String authPort = isTLSEnabled ? System.getProperty("auth.server.https.port", "8543") : System.getProperty("auth.server.http.port", "8180");
        String authScheme = isTLSEnabled ? "https" : "http";
        String authHost = System.getProperty("auth.server.host", "localhost");
        return authScheme + "://" + authHost + ":" + authPort + "/auth";
    }
}
