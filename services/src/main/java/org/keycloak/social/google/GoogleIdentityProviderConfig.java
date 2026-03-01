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
    public void validate(RealmModel realm) {
        if (!GoogleIdentityProvider.ISSUER_URL.equals(getConfig().get(ISSUER))) {
           throw new IllegalArgumentException("The issuer url [" + getConfig().get(ISSUER) + "] is invalid");
        }
        if (isJWTAuthorizationGrantEnabled()) {
            validateIssuer(realm);
        }
    }
}
