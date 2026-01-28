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

package org.keycloak.models;


import java.util.Map;

import org.keycloak.common.util.Time;
import org.keycloak.sessions.CommonClientSessionModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface AuthenticatedClientSessionModel extends CommonClientSessionModel {

    final String STARTED_AT_NOTE = "startedAt";
    final String USER_SESSION_STARTED_AT_NOTE = "userSessionStartedAt";
    final String USER_SESSION_REMEMBER_ME_NOTE = "userSessionRememberMe";
    final String REFRESH_TOKEN_PREFIX = "refreshTokenPrefix";
    final String REFRESH_TOKEN_USE_PREFIX = "refreshTokenUsePrefix";
    final String REFRESH_TOKEN_LAST_REFRESH_PREFIX = "refreshTokenLastRefreshPrefix";

    String getId();

    default int getStarted() {
        String started = getNote(STARTED_AT_NOTE);
        if (started == null) {
            // Note can be null for offline sessions migrated from old version where "startedAt" note was not yet available
            // Fallback to user session started for offline or 0
            return getUserSession().isOffline() ? getUserSessionStarted() : 0;
        }
        return Integer.parseInt(started);
    }

    default int getUserSessionStarted() {
        String started = getNote(USER_SESSION_STARTED_AT_NOTE);
        return started == null ? getUserSession().getStarted() : Integer.parseInt(started);
    }

    default boolean isUserSessionRememberMe() {
        return Boolean.parseBoolean(getNote(USER_SESSION_REMEMBER_ME_NOTE));
    }

    /**
     * @deprecated for removed, without replacement.
     */
    @Deprecated(since = "26.4", forRemoval = true)
    // This data may not be required as we can check the expiry time in the refresh token. 
    // If so, this method can be removed; otherwise we keep the method and remove the deprecation notice.
    int getTimestamp();

    /**
     * Set the timestamp for the client session.
     * If the timestamp is smaller or equal than the current timestamp, the operation is ignored.
     * @deprecated for removed, without replacement.
     */
    @Deprecated(since = "26.4", forRemoval = true)
    void setTimestamp(int timestamp);

    /**
     * Detaches the client session from its user session.
     */
    void detachFromUserSession();
    UserSessionModel getUserSession();

    /**
     * @deprecated use {@link #getRefreshToken(String)}
     */
    @Deprecated
    default String getCurrentRefreshToken() {
        return null;
    }

    /**
     *  @deprecated use {@link #setRefreshToken(String, String)}}
     */
    @Deprecated
    default void setCurrentRefreshToken(String currentRefreshToken) {
    }

    /**
     * @deprecated use {@link #getRefreshTokenUseCount(String)}
     */
    @Deprecated
    default int getCurrentRefreshTokenUseCount() {
        return 0;
    }

    /**
     * @deprecated  use {@link #setRefreshTokenUseCount(String, int)}
     */
    @Deprecated
    default void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
    }

    default String getRefreshToken(String reuseId) {
        return getNote(REFRESH_TOKEN_PREFIX + reuseId);
    }
    default void setRefreshToken(String reuseId, String refreshTokenId) {
        setNote(REFRESH_TOKEN_PREFIX + reuseId, refreshTokenId);
    }
    default int getRefreshTokenUseCount(String reuseId) {
        String count = getNote(REFRESH_TOKEN_USE_PREFIX + reuseId);
        return count == null ? 0 : Integer.parseInt(count);
    }
    default void setRefreshTokenUseCount(String reuseId, int refreshTokenUseCount) {
        setNote(REFRESH_TOKEN_USE_PREFIX + reuseId, String.valueOf(refreshTokenUseCount));
    }
    default int getRefreshTokenLastRefresh(String reuseId) {
        String timestamp = getNote(REFRESH_TOKEN_LAST_REFRESH_PREFIX + reuseId);
        return timestamp == null ? 0 : Integer.parseInt(timestamp);
    }
    default void setRefreshTokenLastRefresh(String reuseId, int refreshTokenLastRefresh) {
        setNote(REFRESH_TOKEN_LAST_REFRESH_PREFIX + reuseId, String.valueOf(refreshTokenLastRefresh));
    }

    String getNote(String name);
    void setNote(String name, String value);
    void removeNote(String name);
    Map<String, String> getNotes();

    default void restartClientSession() {
        setAction(null);
        setRedirectUri(null);
        setTimestamp(Time.currentTime());
        for (String note : getNotes().keySet()) {
            if (!AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE.equals(note)
                    && !AuthenticatedClientSessionModel.STARTED_AT_NOTE.equals(note)
                    && !AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE.equals(note)) {
                removeNote(note);
            }
        }
        setNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(getTimestamp()));
    }
}
