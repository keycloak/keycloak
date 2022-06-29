/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.userSession;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.map.common.TimeAdapter;

import java.util.Collections;
import java.util.Map;

import static org.keycloak.models.map.userSession.SessionExpiration.setClientSessionExpiration;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public abstract class MapAuthenticatedClientSessionAdapter extends AbstractAuthenticatedClientSessionModel {

    public MapAuthenticatedClientSessionAdapter(KeycloakSession session, RealmModel realm,
                                                UserSessionModel userSession, MapAuthenticatedClientSessionEntity entity) {
        super(session, realm, userSession, entity);
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public int getTimestamp() {
        Long timestamp = entity.getTimestamp();
        return timestamp != null ? TimeAdapter.fromLongWithTimeInSecondsToIntegerWithTimeInSeconds(TimeAdapter.fromMilliSecondsToSeconds(timestamp)) : 0;
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(TimeAdapter.fromSecondsToMilliseconds(timestamp));

        // whenever the timestamp is changed recompute the expiration time
        setClientSessionExpiration(entity, realm, getClient());
    }

    @Override
    public UserSessionModel getUserSession() {
        return userSession;
    }

    @Override
    public String getCurrentRefreshToken() {
        return entity.getCurrentRefreshToken();
    }

    @Override
    public void setCurrentRefreshToken(String currentRefreshToken) {
        entity.setCurrentRefreshToken(currentRefreshToken);
    }

    @Override
    public int getCurrentRefreshTokenUseCount() {
        Integer currentRefreshTokenUseCount = entity.getCurrentRefreshTokenUseCount();
        return currentRefreshTokenUseCount != null ? currentRefreshTokenUseCount : 0;
    }

    @Override
    public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
        entity.setCurrentRefreshTokenUseCount(currentRefreshTokenUseCount);
    }

    @Override
    public String getNote(String name) {
        return (name != null) ? entity.getNote(name) : null;
    }

    @Override
    public void setNote(String name, String value) {
        if (name != null) {
            if (value == null) {
                entity.removeNote(name);
            } else {
                entity.setNote(name, value);
            }
        }
    }

    @Override
    public void removeNote(String name) {
        if (name != null) {
            entity.removeNote(name);
        }
    }

    @Override
    public Map<String, String> getNotes() {
        Map<String, String> notes = entity.getNotes();
        return notes == null ? Collections.emptyMap() : Collections.unmodifiableMap(notes);
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        entity.setRedirectUri(uri);
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ClientModel getClient() {
        return realm.getClientById(entity.getClientId());
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        entity.setAction(action);
    }

    @Override
    public String getProtocol() {
        return entity.getAuthMethod();
    }

    @Override
    public void setProtocol(String method) {
        entity.setAuthMethod(method);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), hashCode());
    }
}
