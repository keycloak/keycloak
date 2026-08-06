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
package org.keycloak.social.google;

import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantConfig;
import org.keycloak.broker.oidc.IssuerValidation;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class GoogleIdentityProviderConfig extends OIDCIdentityProviderConfig implements JWTAuthorizationGrantConfig, IssuerValidation {

    public GoogleIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public GoogleIdentityProviderConfig() {
        
    }

    public boolean isUserIp() {
        String userIp = getConfig().get("userIp");
        return userIp == null ? false : Boolean.valueOf(userIp);
    }

    public void setUserIp(boolean ip) {
        getConfig().put("userIp", String.valueOf(ip));
    }

    public String getHostedDomain() {
        String hostedDomain = getConfig().get("hostedDomain");

        return hostedDomain == null || hostedDomain.isEmpty() ? null : hostedDomain;
    }

    public void setHostedDomain(final String hostedDomain) {
        getConfig().put("hostedDomain", hostedDomain);
    }

    public boolean isOfflineAccess() {
        String offlineAccess = getConfig().get("offlineAccess");
        return offlineAccess == null ? false : Boolean.valueOf(offlineAccess);
    }
    
    public void setOfflineAccess(boolean offlineAccess) {
        getConfig().put("offlineAccess", String.valueOf(offlineAccess));
    }

    @Override
    public int getJWTAuthorizationGrantMaxAllowedAssertionExpiration() {
        return Integer.parseInt(getConfig().getOrDefault(JWT_AUTHORIZATION_GRANT_MAX_ALLOWED_ASSERTION_EXPIRATION, "3600"));
    }

    @Override
    public void validate(KeycloakSession session, RealmModel realm) {
        // Google has fixed, well-known endpoints, so it intentionally replaces the generic OIDC URL/PKCE
        // validation (super.validate) with an issuer-only check rather than extending it. The mTLS
        // (tls_client_auth) validation, however, is not Google-specific: a Google IdP created/updated via
        // REST or import with tls_client_auth must be rejected here if it has no usable client-certificate
        // key provider, instead of being persisted and then deterministically failing on the first
        // backchannel request. So we invoke the extracted mTLS checks explicitly while preserving the
        // issuer-only exception above.
        if (!GoogleIdentityProvider.ISSUER_URL.equals(getConfig().get(ISSUER))) {
           throw new IllegalArgumentException("The issuer url [" + getConfig().get(ISSUER) + "] is invalid");
        }
        if (isJWTAuthorizationGrantEnabled()) {
            validateIssuer(realm, IdentityProviderType.JWT_AUTHORIZATION_GRANT);
        }
        if (isTlsClientAuth()) {
            validateTlsClientAuth(realm);
            validateTlsClientAuthKeyResolution(session, realm);
        }
    }
}
