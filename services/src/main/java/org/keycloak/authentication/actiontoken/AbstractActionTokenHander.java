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

import org.keycloak.Config.Scope;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 *
 * @author hmlnarik
 */
public abstract class AbstractActionTokenHander<T extends JsonWebToken> implements ActionTokenHandler<T>, ActionTokenHandlerFactory<T> {

    private final String id;
    private final Class<T> tokenClass;
    private final String defaultErrorMessage;
    private final EventType defaultEventType;
    private final String defaultEventError;

    public AbstractActionTokenHander(String id, Class<T> tokenClass, String defaultErrorMessage, EventType defaultEventType, String defaultEventError) {
        this.id = id;
        this.tokenClass = tokenClass;
        this.defaultErrorMessage = defaultErrorMessage;
        this.defaultEventType = defaultEventType;
        this.defaultEventError = defaultEventError;
    }

    @Override
    public ActionTokenHandler<T> create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void close() {
    }

    @Override
    public Class<T> getTokenClass() {
        return this.tokenClass;
    }

    @Override
    public EventType eventType() {
        return this.defaultEventType;
    }

    @Override
    public String getDefaultErrorMessage() {
        return this.defaultErrorMessage;
    }

    @Override
    public String getDefaultEventError() {
        return this.defaultEventError;
    }

    @Override
    public String getAuthenticationSessionIdFromToken(T token, ActionTokenContext<T> tokenContext, AuthenticationSessionModel currentAuthSession) {
        return token instanceof DefaultActionToken ? ((DefaultActionToken) token).getCompoundAuthenticationSessionId() : null;
    }

    @Override
    public AuthenticationSessionModel startFreshAuthenticationSession(T token, ActionTokenContext<T> tokenContext) {
        AuthenticationSessionModel authSession = tokenContext.createAuthenticationSessionForClient(token.getIssuedFor());
        authSession.setAuthNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        return authSession;
    }
    
    @Override
    public boolean canUseTokenRepeatedly(T token, ActionTokenContext<T> tokenContext) {
        return true;
    }
}
