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

package org.keycloak.authentication.authenticators.browser.risk.context;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

public final class LoginContext {

    private final String realmId;
    private final String userId;
    private final String username;
    private final String remoteAddress;
    private final String userAgent;
    private final String acceptLanguage;
    private final String geoSignal;
    private final Instant loginTime;
    private final int recentFailedAttempts;
    private final Set<String> recentSuccessfulLoginIps;
    private final Set<String> recentDeviceFingerprints;
    private final Set<String> recentGeoSignals;
    private final boolean historyAvailable;

    public LoginContext(String realmId, String userId, String username, String remoteAddress, String userAgent,
            String acceptLanguage, String geoSignal, Instant loginTime, int recentFailedAttempts,
            Set<String> recentSuccessfulLoginIps, Set<String> recentDeviceFingerprints, Set<String> recentGeoSignals,
            boolean historyAvailable) {
        this.realmId = realmId;
        this.userId = userId;
        this.username = username;
        this.remoteAddress = remoteAddress;
        this.userAgent = userAgent;
        this.acceptLanguage = acceptLanguage;
        this.geoSignal = geoSignal;
        this.loginTime = loginTime == null ? Instant.now() : loginTime;
        this.recentFailedAttempts = Math.max(0, recentFailedAttempts);
        this.recentSuccessfulLoginIps = immutableSet(recentSuccessfulLoginIps);
        this.recentDeviceFingerprints = immutableSet(recentDeviceFingerprints);
        this.recentGeoSignals = immutableSet(recentGeoSignals);
        this.historyAvailable = historyAvailable;
    }

    private static Set<String> immutableSet(Set<String> values) {
        return values == null ? Collections.emptySet() : Set.copyOf(values);
    }

    public String getRealmId() {
        return realmId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getAcceptLanguage() {
        return acceptLanguage;
    }

    public String getGeoSignal() {
        return geoSignal;
    }

    public Instant getLoginTime() {
        return loginTime;
    }

    public int getRecentFailedAttempts() {
        return recentFailedAttempts;
    }

    public Set<String> getRecentSuccessfulLoginIps() {
        return recentSuccessfulLoginIps;
    }

    public Set<String> getRecentDeviceFingerprints() {
        return recentDeviceFingerprints;
    }

    public Set<String> getRecentGeoSignals() {
        return recentGeoSignals;
    }

    public boolean isHistoryAvailable() {
        return historyAvailable;
    }
}
