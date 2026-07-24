/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;

import java.util.Collections;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import static org.keycloak.models.sessions.infinispan.ImmutableSession.readOnly;

/**
 * An immutable {@link AuthenticatedClientSessionModel} implementation.
 * <p>
 * All setters throw a {@link UnsupportedOperationException}.
 */
record ImmutableClientSession(
        String id,
        ClientModel client,
        ImmutableUserSessionModel userSessionModel,
        Map<String, String> notes,
        String redirectUri,
        String action,
        String protocol,
        int timestamp,
        int started
) implements AuthenticatedClientSessionModel {


    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(int timestamp) {
        readOnly();
    }

    @Override
    public int getStarted() {
        return started;
    }

    @Override
    public void detachFromUserSession() {
        readOnly();
    }

    @Override
    public UserSessionModel getUserSession() {
        return userSessionModel;
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    @Override
    public void setNote(String name, String value) {
        readOnly();
    }

    @Override
    public void removeNote(String name) {
        readOnly();
    }

    @Override
    public Map<String, String> getNotes() {
        return Collections.unmodifiableMap(notes);
    }

    @Override
    public String getRedirectUri() {
        return redirectUri;
    }

    @Override
    public void setRedirectUri(String uri) {
        readOnly();
    }

    @Override
    public RealmModel getRealm() {
        return userSessionModel().getRealm();
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(String action) {
        readOnly();
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String method) {
        readOnly();
    }
}
