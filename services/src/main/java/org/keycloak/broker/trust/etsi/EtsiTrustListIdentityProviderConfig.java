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

package org.keycloak.broker.trust.etsi;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import org.keycloak.common.util.PemUtils;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.Strings;

import static org.keycloak.common.util.UriUtils.checkUrl;

public class EtsiTrustListIdentityProviderConfig extends IdentityProviderModel {

    public static final String TRUST_LIST_URL = "trustListUrl";
    public static final String SIGNING_CERTIFICATES = "trustListSigningCertificates";
    public static final String LOTE_TYPE = "trustListLoteType";
    public static final String SERVICE_TYPES = "trustListServiceTypes";
    public static final String SERVICE_STATUSES = "trustListServiceStatuses";
    public static final String REFRESH_INTERVAL = "trustListRefreshInterval";

    public static final int DEFAULT_REFRESH_INTERVAL = 3600;

    public EtsiTrustListIdentityProviderConfig() {
    }

    public EtsiTrustListIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    @Override
    public Boolean isHideOnLogin() {
        return true;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        if (Strings.isEmpty(getTrustListUrl())) {
            throw new IllegalArgumentException("The trust list URL is required");
        }
        checkUrl(realm.getSslRequired(), getTrustListUrl(), TRUST_LIST_URL);
        if (Strings.isEmpty(getLoteType())) {
            throw new IllegalArgumentException("The trust list LoTE type is required");
        }
        if (getServiceTypes().isEmpty()) {
            throw new IllegalArgumentException("At least one trust list service type is required");
        }
        try {
            if (getSigningCertificates().isEmpty()) {
                throw new IllegalArgumentException("At least one trust list signing certificate is required");
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The trust list signing certificates must be a valid PEM bundle", e);
        }
        if (getRefreshInterval() <= 0) {
            throw new IllegalArgumentException("The trust list refresh interval must be a positive number of seconds");
        }
    }

    public String getTrustListUrl() {
        return getConfig().get(TRUST_LIST_URL);
    }

    public List<X509Certificate> getSigningCertificates() {
        String certificates = getConfig().get(SIGNING_CERTIFICATES);
        if (Strings.isEmpty(certificates)) {
            return List.of();
        }
        return List.of(PemUtils.decodeCertificates(certificates));
    }

    public String getLoteType() {
        return getConfig().get(LOTE_TYPE);
    }

    public int getRefreshInterval() {
        String refreshInterval = getConfig().get(REFRESH_INTERVAL);
        if (Strings.isEmpty(refreshInterval)) {
            return DEFAULT_REFRESH_INTERVAL;
        }
        try {
            return Integer.parseInt(refreshInterval.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public List<String> getServiceTypes() {
        return getList(SERVICE_TYPES);
    }

    public List<String> getServiceStatuses() {
        return getList(SERVICE_STATUSES);
    }

    private List<String> getList(String key) {
        String configured = getConfig().get(key);
        if (Strings.isEmpty(configured)) {
            return List.of();
        }
        return Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
