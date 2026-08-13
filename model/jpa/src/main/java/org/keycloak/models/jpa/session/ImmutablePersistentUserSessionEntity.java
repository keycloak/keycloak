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

package org.keycloak.models.jpa.session;

import org.keycloak.models.session.PersistentUserSessionModel;

/**
 * An immutable {@link PersistentUserSessionEntity} to optimize read-only queries.
 */
public record ImmutablePersistentUserSessionEntity(
        String userSessionId,
        String realmId,
        String userId,
        int createOn,
        int lastSessionRefresh,
        String brokerSessionId,
        String offline,
        String data,
        Boolean rememberMe
) implements PersistentUserSessionModel {
    @Override
    public String getUserSessionId() {
        return userSessionId;
    }

    @Override
    public void setUserSessionId(String userSessionId) {
        readOnly();
    }

    @Override
    public int getStarted() {
        return createOn;
    }

    @Override
    public void setStarted(int started) {
        readOnly();
    }

    @Override
    public int getLastSessionRefresh() {
        return lastSessionRefresh;
    }

    @Override
    public void setLastSessionRefresh(int lastSessionRefresh) {
        readOnly();
    }

    @Override
    public boolean isOffline() {
        return JpaSessionUtil.offlineFromString(offline);
    }

    @Override
    public void setOffline(boolean offline) {
        readOnly();
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public void setData(String data) {
        readOnly();
    }

    @Override
    public void setRealmId(String realmId) {
        readOnly();
    }

    @Override
    public void setUserId(String userId) {
        readOnly();
    }

    @Override
    public void setBrokerSessionId(String brokerSessionId) {
        readOnly();
    }

    @Override
    public boolean isRememberMe() {
        return rememberMe == Boolean.TRUE;
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        readOnly();
    }

    private static void readOnly() {
        throw new UnsupportedOperationException("this instance is read-only");
    }
}
