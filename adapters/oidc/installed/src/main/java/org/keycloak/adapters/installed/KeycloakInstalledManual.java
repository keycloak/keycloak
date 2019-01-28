/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.installed;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.installed.core.AbstractKeycloakInstalled;
import org.keycloak.common.VerificationException;

class KeycloakInstalledManual extends AbstractKeycloakInstalled {

    public KeycloakInstalledManual() {
    }

    public KeycloakInstalledManual(InputStream config) {
        super(config);
    }

    public KeycloakInstalledManual(KeycloakDeployment deployment) {
        super(deployment);
    }

    public void login() throws IOException, ServerRequest.HttpFailure, VerificationException {
        login(System.out, new InputStreamReader(System.in));
    }

    public void login(PrintStream printer, Reader reader) throws IOException, ServerRequest.HttpFailure, VerificationException {
        String redirectUri = "urn:ietf:wg:oauth:2.0:oob";

        String authUrl = getDeployment().getAuthUrl().clone()
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OAuth2Constants.CODE)
                .queryParam(OAuth2Constants.CLIENT_ID, getDeployment().getResourceName())
                .queryParam(OAuth2Constants.REDIRECT_URI, redirectUri)
                .queryParam(OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID)
                .build().toString();

        printer.println("Open the following URL in a browser. After login copy/paste the code back and press <enter>");
        printer.println(authUrl);
        printer.println();
        printer.print("Code: ");

        String code = readCode(reader);
        processCode(code, redirectUri);
    }

    private String readCode(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();

        char cb[] = new char[1];
        while (reader.read(cb) != -1) {
            char c = cb[0];
            if ((c == ' ') || (c == '\n') || (c == '\r')) {
                break;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }


}
