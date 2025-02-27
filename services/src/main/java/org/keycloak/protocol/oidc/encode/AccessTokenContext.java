/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.encode;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Some context info about the token
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AccessTokenContext {

    private final SessionType sessionType;
    private final TokenType tokenType;
    private final String grantType;
    private final String rawTokenId;

    public enum SessionType {
        ONLINE("on"),
        OFFLINE("of"),
        TRANSIENT("tr"),
        UNKNOWN("un");

        private final String shortcut;

        SessionType(String shortcut) {
            this.shortcut = shortcut;
        }

        public String getShortcut() {
            return shortcut;
        }
    }

    public enum TokenType {
        REGULAR("rt"),
        LIGHTWEIGHT("lt"),
        UNKNOWN("un");

        private final String shortcut;

        TokenType(String shortcut) {
            this.shortcut = shortcut;
        }

        public String getShortcut() {
            return shortcut;
        }
    }

    @JsonCreator
    public AccessTokenContext(@JsonProperty("sessionType") SessionType sessionType, @JsonProperty("tokenType") TokenType tokenType, @JsonProperty("grantType") String grantType, @JsonProperty("rawTokenId") String rawTokenId) {
        Objects.requireNonNull(sessionType, "Null sessionType not allowed");
        Objects.requireNonNull(tokenType, "Null tokenType not allowed");
        Objects.requireNonNull(grantType, "Null grantType not allowed");
        Objects.requireNonNull(grantType, "Null rawTokenId not allowed");
        this.sessionType = sessionType;
        this.tokenType = tokenType;
        this.grantType = grantType;
        this.rawTokenId = rawTokenId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getRawTokenId() {
        return rawTokenId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof AccessTokenContext that &&
                sessionType == that.sessionType &&
                tokenType == that.tokenType &&
                Objects.equals(grantType, that.grantType) &&
                Objects.equals(rawTokenId, that.rawTokenId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionType, tokenType, grantType, rawTokenId);
    }
}
