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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.DeviceRiskStrategy;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

public class LoginContextCollector {

    public static final String HEADER_CF_IP_COUNTRY = "CF-IPCountry";
    public static final String HEADER_CLOUDFRONT_VIEWER_COUNTRY = "CloudFront-Viewer-Country";
    public static final String HEADER_X_FORWARDED_COUNTRY = "X-Forwarded-Country";
    public static final String EVENT_DETAIL_DEVICE = "risk_device";
    public static final String EVENT_DETAIL_GEO = "risk_geo";

    public LoginContext collect(AuthenticationFlowContext context, AdaptiveAuthPolicy policy) {
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        MultivaluedMap<String, String> headers = requestHeaders(context);
        String userAgent = firstHeader(headers, HttpHeaders.USER_AGENT);
        String acceptLanguage = firstHeader(headers, HttpHeaders.ACCEPT_LANGUAGE);
        String geoSignal = geoSignalFromHeaders(headers);
        String userId = user == null ? null : user.getId();
        History history = collectHistory(context, realm, userId, policy.getHistoryLookbackLimit());

        return new LoginContext(
                realm == null ? null : realm.getId(),
                userId,
                user == null ? null : user.getUsername(),
                context.getConnection() == null ? null : context.getConnection().getRemoteAddr(),
                userAgent,
                acceptLanguage,
                geoSignal,
                Instant.now(),
                recentFailedAttempts(context, realm, userId),
                history.ips,
                history.devices,
                history.geos,
                history.available);
    }

    private static MultivaluedMap<String, String> requestHeaders(AuthenticationFlowContext context) {
        HttpRequest request = context.getHttpRequest();
        if (request == null || request.getHttpHeaders() == null || request.getHttpHeaders().getRequestHeaders() == null) {
            return new MultivaluedHashMap<>();
        }
        return request.getHttpHeaders().getRequestHeaders();
    }

    static String geoSignalFromHeaders(MultivaluedMap<String, String> headers) {
        String value = firstHeader(headers, HEADER_CF_IP_COUNTRY);
        if (!StringUtil.isBlank(value)) {
            return normalizeGeo(value);
        }

        value = firstHeader(headers, HEADER_CLOUDFRONT_VIEWER_COUNTRY);
        if (!StringUtil.isBlank(value)) {
            return normalizeGeo(value);
        }

        value = firstHeader(headers, HEADER_X_FORWARDED_COUNTRY);
        return StringUtil.isBlank(value) ? null : normalizeGeo(value);
    }

    static String firstHeader(MultivaluedMap<String, String> headers, String name) {
        if (headers == null || StringUtil.isBlank(name)) {
            return null;
        }

        String direct = headers.getFirst(name);
        if (!StringUtil.isBlank(direct)) {
            return direct;
        }

        for (Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }

        return null;
    }

    private static String normalizeGeo(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private int recentFailedAttempts(AuthenticationFlowContext context, RealmModel realm, String userId) {
        if (realm == null || userId == null) {
            return 0;
        }

        try {
            UserLoginFailureModel failure = context.getSession().loginFailures().getUserLoginFailure(realm, userId);
            return failure == null ? 0 : Math.max(0, failure.getNumFailures());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private History collectHistory(AuthenticationFlowContext context, RealmModel realm, String userId, int limit) {
        if (realm == null || userId == null || limit <= 0) {
            return History.unavailable();
        }

        EventStoreProvider eventStore;
        try {
            eventStore = context.getSession().getProvider(EventStoreProvider.class);
        } catch (RuntimeException e) {
            return History.unavailable();
        }

        if (eventStore == null) {
            return History.unavailable();
        }

        try {
            History history = new History(true);
            eventStore.createQuery()
                    .realm(realm.getId())
                    .user(userId)
                    .type(EventType.LOGIN)
                    .orderByDescTime()
                    .maxResults(limit)
                    .getResultStream()
                    .forEach(event -> addEvent(history, event));
            return history;
        } catch (RuntimeException e) {
            return History.unavailable();
        }
    }

    private static void addEvent(History history, Event event) {
        if (!StringUtil.isBlank(event.getIpAddress())) {
            history.ips.add(event.getIpAddress().trim());
        }

        Map<String, String> details = event.getDetails();
        if (details == null) {
            return;
        }

        String device = details.get(EVENT_DETAIL_DEVICE);
        if (!StringUtil.isBlank(device)) {
            history.devices.add(device);
        }

        String geo = details.get(EVENT_DETAIL_GEO);
        if (!StringUtil.isBlank(geo)) {
            history.geos.add(normalizeGeo(geo));
        }
    }

    public static String currentDeviceFingerprint(String userAgent, String acceptLanguage) {
        return DeviceRiskStrategy.fingerprint(userAgent, acceptLanguage);
    }

    private static final class History {
        private final Set<String> ips = new HashSet<>();
        private final Set<String> devices = new HashSet<>();
        private final Set<String> geos = new HashSet<>();
        private final boolean available;

        private History(boolean available) {
            this.available = available;
        }

        private static History unavailable() {
            return new History(false);
        }
    }
}
