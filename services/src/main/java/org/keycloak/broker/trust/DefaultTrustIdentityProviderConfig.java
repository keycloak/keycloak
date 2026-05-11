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

package org.keycloak.broker.trust;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.Strings;

import static org.keycloak.common.util.UriUtils.checkUrl;

public class DefaultTrustIdentityProviderConfig extends IdentityProviderModel {

    public static final String TRUSTED_JWKS_URL = "trustedJwksUrl";
    public static final String TRUSTED_JWKS = "trustedJwks";

    public DefaultTrustIdentityProviderConfig() {
    }

    public DefaultTrustIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    @Override
    public Boolean isHideOnLogin() {
        return true;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        boolean hasTrustedJwksUrl = !Strings.isEmpty(getTrustedJwksUrl());
        boolean hasTrustedJwks = !Strings.isEmpty(getTrustedJwks());
        if (hasTrustedJwksUrl == hasTrustedJwks) {
            throw new IllegalArgumentException("Configure exactly one of trusted JWKS URL or trusted JWKS");
        }
        if (hasTrustedJwksUrl) {
            checkUrl(realm.getSslRequired(), getTrustedJwksUrl(), TRUSTED_JWKS_URL);
        }
    }

    public String getTrustedJwksUrl() {
        return getConfig().get(TRUSTED_JWKS_URL);
    }

    public void setTrustedJwksUrl(String trustedJwksUrl) {
        if (trustedJwksUrl == null) {
            getConfig().remove(TRUSTED_JWKS_URL);
        } else {
            getConfig().put(TRUSTED_JWKS_URL, trustedJwksUrl);
        }
    }

    public String getTrustedJwks() {
        return getConfig().get(TRUSTED_JWKS);
    }

    public void setTrustedJwks(String trustedJwks) {
        if (trustedJwks == null) {
            getConfig().remove(TRUSTED_JWKS);
        } else {
            getConfig().put(TRUSTED_JWKS, trustedJwks);
        }
    }
}
