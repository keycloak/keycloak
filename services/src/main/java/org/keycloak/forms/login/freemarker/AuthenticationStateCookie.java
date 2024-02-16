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

package org.keycloak.forms.login.freemarker;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Encode;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

/**
 * Non http-only cookie with tracking remaining authSessions in current root authentication session
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationStateCookie {

    private static final Logger logger = Logger.getLogger(AuthenticationStateCookie.class);

    public static final String KC_AUTH_STATE = "KC_AUTH_STATE";

    @JsonProperty("authSessionId")
    private String authSessionId;

    @JsonProperty("remainingTabs")
    private Set<String> remainingTabs;

    public String getAuthSessionId() {
        return authSessionId;
    }

    public void setAuthSessionId(String authSessionId) {
        this.authSessionId = authSessionId;
    }

    public Set<String> getRemainingTabs() {
        return remainingTabs;
    }

    public void setRemainingTabs(Set<String> remainingTabs) {
        this.remainingTabs = remainingTabs;
    }

    public static void generateAndSetCookie(KeycloakSession session, RootAuthenticationSessionModel rootAuthSession, int cookieMaxAge) {
        AuthenticationStateCookie cookie = new AuthenticationStateCookie();
        cookie.setAuthSessionId(rootAuthSession.getId());
        cookie.setRemainingTabs(rootAuthSession.getAuthenticationSessions().keySet());

        try {
            String encoded = Encode.urlEncode(JsonSerialization.writeValueAsString(cookie));
            session.getProvider(CookieProvider.class).set(CookieType.AUTH_STATE, encoded, cookieMaxAge);
        } catch (IOException ioe) {
            throw new IllegalStateException("Exception thrown when encoding cookie", ioe);
        }
    }

    public static void expireCookie(KeycloakSession session) {
        session.getProvider(CookieProvider.class).expire(CookieType.AUTH_STATE);
    }

    @Override
    public String toString() {
        return new StringBuilder("AuthenticationStateCookie [ ")
                .append("authSessionId=" + authSessionId)
                .append(", remainingTabs=" + remainingTabs)
                .append(" ]")
                .toString();
    }
}
