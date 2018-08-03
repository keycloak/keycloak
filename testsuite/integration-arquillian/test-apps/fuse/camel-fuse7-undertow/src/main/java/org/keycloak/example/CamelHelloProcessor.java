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

package org.keycloak.example;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CamelHelloProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        // Fuse 7
        KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) exchange.getProperty(KeycloakPrincipal.class.getName(), KeycloakPrincipal.class);

        if (keycloakPrincipal == null) {
            // Fuse 6.3
            HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
            keycloakPrincipal = (KeycloakPrincipal) req.getUserPrincipal();
        }

        AccessToken accessToken = keycloakPrincipal.getKeycloakSecurityContext().getToken();
        String username = accessToken.getPreferredUsername();
        String fullName = accessToken.getName();

        exchange.getOut().setBody("Hello " + username + "! Your full name is " + fullName + ".");
    }
}
