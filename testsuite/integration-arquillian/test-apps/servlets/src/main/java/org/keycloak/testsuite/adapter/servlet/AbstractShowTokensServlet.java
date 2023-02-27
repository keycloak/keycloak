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

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.RefreshToken;
import org.keycloak.util.JsonSerialization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractShowTokensServlet extends HttpServlet {

    private static final String LINK = "<a href=\"%s\" id=\"%s\">%s</a>";

    protected String renderTokens(HttpServletRequest req)  throws ServletException, IOException {
        RefreshableKeycloakSecurityContext ctx = (RefreshableKeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
        String accessTokenPretty = JsonSerialization.writeValueAsPrettyString(ctx.getToken());
        RefreshToken refreshToken;
        try {
            refreshToken = new JWSInput(ctx.getRefreshToken()).readJsonContent(RefreshToken.class);
        } catch (JWSInputException e) {
            throw new IOException(e);
        }
        String refreshTokenPretty = JsonSerialization.writeValueAsPrettyString(refreshToken);

        return new StringBuilder("<span id=\"accessToken\">" + accessTokenPretty + "</span>")
                .append("<span id=\"refreshToken\">" + refreshTokenPretty + "</span>")
                .append("<span id=\"accessTokenString\">" + ctx.getTokenString() + "</span>")
                .append("<span id=\"refreshTokenString\">" + ctx.getRefreshToken() + "</span>")
                .toString();
    }


}
