/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.actiontoken;

import org.keycloak.TokenVerifier.Predicate;
import org.keycloak.common.VerificationException;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.provider.Provider;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionModel;
import javax.ws.rs.core.Response;

/**
 *  Handler of the action token.
 *
 * @param <T> Class implementing the action token
 *
 *  @author hmlnarik
 */
public interface ActionTokenHandler<T extends JsonWebToken> extends Provider {

    /**
     * Performs the action as per the token details. This method is only called if all verifiers
     * returned in {@link #handleToken} succeed.
     *
     * @param token
     * @param tokenContext
     * @return
     */
    Response handleToken(T token, ActionTokenContext<T> tokenContext);

    /**
     * Returns the Java token class for use with deserialization.
     * @return
     */
    Class<T> getTokenClass();

    /**
     * Returns an array of verifiers that are tested prior to handling the token. All verifiers have to pass successfully
     * for token to be handled. The returned array must not be {@code null}.
     * @param tokenContext
     * @return Verifiers or an empty array. The returned array must not be {@code null}.
     */
    default Predicate<? super T>[] getVerifiers(ActionTokenContext<T> tokenContext) {
        return new Predicate[] {};
    }

    /**
     * Returns a compound authentication session ID requested from within the given token that the handler should attempt to join.
     * @param token Token. Can be {@code null}
     * @param tokenContext
     * @param currentAuthSession Authentication session that is currently in progress, {@code null} if no authentication session is not set
     * @see AuthenticationSessionCompoundId
     * @return Authentication session ID (can be {@code null} if the token does not contain authentication session ID)
     */
    String getAuthenticationSessionIdFromToken(T token, ActionTokenContext<T> tokenContext, AuthenticationSessionModel currentAuthSession);

    /**
     * Returns a event type logged with {@link EventBuilder} class.
     * @return
     */
    EventType eventType();

    /**
     * Returns an error to be shown in the {@link EventBuilder} detail when token handling fails and
     * no more specific error is provided.
     * @return
     */
    String getDefaultEventError();

    /**
     * Returns an error to be shown in the response when token handling fails and no more specific
     * error message is provided.
     * @return
     */
    String getDefaultErrorMessage();

    /**
     * Creates a fresh authentication session according to the information from the token. The default
     * implementation creates a new authentication session that requests termination after required actions.
     * @param token
     * @param tokenContext
     * @return
     */
    AuthenticationSessionModel startFreshAuthenticationSession(T token, ActionTokenContext<T> tokenContext) throws VerificationException;

    /**
     * Returns {@code true} when the token can be used repeatedly to invoke the action, {@code false} when the token
     * is intended to be for single use only.
     * @return see above
     */
    boolean canUseTokenRepeatedly(T token, ActionTokenContext<T> tokenContext);
}
