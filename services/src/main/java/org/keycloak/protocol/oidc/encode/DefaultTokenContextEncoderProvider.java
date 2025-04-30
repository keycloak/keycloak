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

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DefaultTokenContextEncoderProvider implements TokenContextEncoderProvider {

    public static final String UNKNOWN = "na";

    private final KeycloakSession session;
    private final DefaultTokenContextEncoderProviderFactory factory;

    public DefaultTokenContextEncoderProvider(KeycloakSession session,
                                              DefaultTokenContextEncoderProviderFactory factory) {
        this.session = session;
        this.factory = factory;
    }

    @Override
    public AccessTokenContext getTokenContextFromClientSessionContext(ClientSessionContext clientSessionContext, String rawTokenId) {
        AccessTokenContext.SessionType sessionType;
        UserSessionModel userSession = clientSessionContext.getClientSession().getUserSession();
        if (userSession.getPersistenceState() == UserSessionModel.SessionPersistenceState.TRANSIENT) {
            String createdFromPersistent = userSession.getNote(Constants.CREATED_FROM_PERSISTENT);
            if (createdFromPersistent != null) {
                sessionType = Constants.CREATED_FROM_PERSISTENT_OFFLINE.equals(createdFromPersistent) ? AccessTokenContext.SessionType.OFFLINE_TRANSIENT_CLIENT : AccessTokenContext.SessionType.ONLINE_TRANSIENT_CLIENT;
            } else {
                sessionType = AccessTokenContext.SessionType.TRANSIENT;
            }
        } else {
            sessionType = clientSessionContext.isOfflineTokenRequested() ? AccessTokenContext.SessionType.OFFLINE : AccessTokenContext.SessionType.ONLINE;
        }

        boolean useLightweightToken = AbstractOIDCProtocolMapper.getShouldUseLightweightToken(session);
        AccessTokenContext.TokenType tokenType = useLightweightToken ? AccessTokenContext.TokenType.LIGHTWEIGHT : AccessTokenContext.TokenType.REGULAR;

        String grantType = clientSessionContext.getAttribute(Constants.GRANT_TYPE, String.class);
        if (grantType == null) {
            grantType = UNKNOWN;
        }

        return new AccessTokenContext(sessionType, tokenType, grantType, rawTokenId);
    }

    @Override
    public AccessTokenContext getTokenContextFromTokenId(String encodedTokenId) {
        int indexOf = encodedTokenId.indexOf(':');
        if (indexOf == -1) {
            return new AccessTokenContext(AccessTokenContext.SessionType.UNKNOWN, AccessTokenContext.TokenType.UNKNOWN, UNKNOWN, encodedTokenId);
        } else {
            String encodedContext = encodedTokenId.substring(0, indexOf);
            String rawId = encodedTokenId.substring(indexOf + 1);

            if (encodedContext.length() != 6) {
                throw new IllegalArgumentException("Incorrect token id: '" + encodedTokenId + "'. Expected length of 6.");
            }

            // First 2 chars are "sessionType", next 2 chars "tokenType", last 2 chars "grantType"
            String stShortcut = encodedContext.substring(0, 2);
            String ttShortcut = encodedContext.substring(2, 4);
            String gtShortcut = encodedContext.substring(4, 6);

            AccessTokenContext.SessionType st = factory.getSessionTypeByShortcut(stShortcut);
            if (st == null) {
                throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown value '" + stShortcut + "' for session type");
            }
            AccessTokenContext.TokenType tt = factory.getTokenTypeByShortcut(ttShortcut);
            if (tt == null) {
                throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown value '" + ttShortcut + "' for token type");
            }
            String gt = factory.getGrantTypeByShortcut(gtShortcut);
            if (gt == null) {
                throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown value '" + gtShortcut + "' for grant type");
            }

            return new AccessTokenContext(st, tt, gt, rawId);
        }
    }

    @Override
    public String encodeTokenId(AccessTokenContext tokenContext) {
        if (tokenContext.getSessionType() == AccessTokenContext.SessionType.UNKNOWN) {
            throw new IllegalStateException("Cannot encode token with unknown sessionType");
        }
        if (tokenContext.getTokenType() == AccessTokenContext.TokenType.UNKNOWN) {
            throw new IllegalStateException("Cannot encode token with unknown tokenType");
        }

        String grantShort = factory.getShortcutByGrantType(tokenContext.getGrantType());
        if (grantShort == null) {
            throw new IllegalStateException("Cannot encode token with unknown grantType: " + tokenContext.getGrantType());
        }

        return tokenContext.getSessionType().getShortcut() +
                tokenContext.getTokenType().getShortcut() +
                grantShort +
                ':' + tokenContext.getRawTokenId();
    }

    @Override
    public void close() {

    }
}
