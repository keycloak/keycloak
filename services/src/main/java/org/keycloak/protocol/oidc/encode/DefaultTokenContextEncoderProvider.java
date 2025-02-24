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

import java.util.Map;

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

    public static final String SESSION_TYPE_PREFIX = "st";
    public static final String TOKEN_TYPE_PREFIX = "tt";
    public static final String GRANT_TYPE_PREFIX = "gt";

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
            sessionType = AccessTokenContext.SessionType.TRANSIENT;
        } else {
            sessionType = userSession.isOffline() ? AccessTokenContext.SessionType.OFFLINE : AccessTokenContext.SessionType.ONLINE;
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
            String encodedChunks = encodedTokenId.substring(0, indexOf);
            String rawId = encodedTokenId.substring(indexOf + 1);
            String[] chunks = encodedChunks.split("_");

            AccessTokenContext.SessionType st = null;
            AccessTokenContext.TokenType tt = null;
            String gt = null;

            for (String chunk : chunks) {
                int dotIndex = chunk.indexOf('.');
                if (dotIndex == -1) {
                    throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". No dot present in the chunk: " + chunk);
                }
                String prefix = chunk.substring(0, dotIndex);
                String value = chunk.substring(dotIndex + 1);
                switch (prefix) {
                    case SESSION_TYPE_PREFIX:
                        st = factory.getSessionTypeByShortcut(value);
                        if (st == null) {
                            throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown value '" + chunk + "' for session type");
                        }
                        break;
                    case TOKEN_TYPE_PREFIX:
                        tt = factory.getTokenTypeByShortcut(value);
                        if (tt == null) {
                            throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown value '" + chunk + "' for token type");
                        }
                        break;
                    case GRANT_TYPE_PREFIX:
                        gt = factory.getGrantTypeByShortcut(value);
                        if (gt == null) {
                            throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown value '" + gt + "' for grant type");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Incorrect token id: " + encodedTokenId + ". Unknown prefix: " + prefix);
                }
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

        return SESSION_TYPE_PREFIX + '.' + tokenContext.getSessionType().getShortcut() + '_' +
                TOKEN_TYPE_PREFIX + '.' + tokenContext.getTokenType().getShortcut() + '_' +
                GRANT_TYPE_PREFIX + '.' + grantShort + ':' +
                tokenContext.getRawTokenId();
    }

    @Override
    public void close() {

    }
}
