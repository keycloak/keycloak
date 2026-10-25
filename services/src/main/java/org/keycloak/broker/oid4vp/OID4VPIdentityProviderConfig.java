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

package org.keycloak.broker.oid4vp;

import java.util.Locale;
import java.util.Set;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.utils.StringUtil;

public class OID4VPIdentityProviderConfig extends IdentityProviderModel {

    public static final String DCQL_QUERY = "dcqlQuery";
    public static final String TRUSTED_ISSUER_JWKS = "trustedIssuerJwks";
    public static final String TRUST_MATERIAL_IDPS = "trustMaterialIdps";
    public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
    public static final String WALLET_SCHEME = "walletScheme";
    public static final String SIGNING_KEY_ID = "signingKeyId";
    public static final String RESPONSE_MODE = "responseMode";

    public static final String DEFAULT_WALLET_SCHEME = "openid4vp://";

    // The scheme becomes the href of the wallet button on the login page, so schemes a browser
    // executes or resolves locally must never reach it, however they are configured.
    private static final Set<String> FORBIDDEN_WALLET_SCHEMES = Set.of("javascript", "data", "vbscript", "blob", "file", "about");
    private static final String URI_SCHEME_PATTERN = "[A-Za-z][A-Za-z0-9+.-]*";

    public OID4VPIdentityProviderConfig() {
    }

    public OID4VPIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    // TODO: Revisit if configuring DCQL by hand should stay or eliminated completely by mapper-inferred auto-generation
    public String getDcqlQuery() {
        return getConfig().get(DCQL_QUERY);
    }

    public void setDcqlQuery(String dcqlQuery) {
        getConfig().put(DCQL_QUERY, dcqlQuery);
    }

    public String getTrustedIssuerJwks() {
        return getConfig().get(TRUSTED_ISSUER_JWKS);
    }

    public void setTrustedIssuerJwks(String trustedIssuerJwks) {
        getConfig().put(TRUSTED_ISSUER_JWKS, trustedIssuerJwks);
    }

    public String getTrustMaterialIdps() {
        return getConfig().get(TRUST_MATERIAL_IDPS);
    }

    public void setTrustMaterialIdps(String trustMaterialIdps) {
        getConfig().put(TRUST_MATERIAL_IDPS, trustMaterialIdps);
    }

    public String getPrincipalAttribute() {
        return getConfig().get(PRINCIPAL_ATTRIBUTE);
    }

    public void setPrincipalAttribute(String principalAttribute) {
        getConfig().put(PRINCIPAL_ATTRIBUTE, principalAttribute);
    }

    public String getSigningKeyId() {
        return getConfig().get(SIGNING_KEY_ID);
    }

    public void setSigningKeyId(String signingKeyId) {
        getConfig().put(SIGNING_KEY_ID, signingKeyId);
    }

    public ResponseMode getResponseMode() {
        String value = getConfig().get(RESPONSE_MODE);
        return StringUtil.isBlank(value) ? ResponseMode.DIRECT_POST : ResponseMode.from(value);
    }

    public void setResponseMode(ResponseMode responseMode) {
        getConfig().put(RESPONSE_MODE, responseMode.value());
    }

    public boolean isEncryptedResponse() {
        return getResponseMode() == ResponseMode.DIRECT_POST_JWT;
    }

    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        String responseMode = getConfig().get(RESPONSE_MODE);
        if (StringUtil.isNotBlank(responseMode)) {
            ResponseMode.from(responseMode);
        }
        if (StringUtil.isBlank(getTrustedIssuerJwks()) && StringUtil.isBlank(getTrustMaterialIdps())) {
            throw new IllegalArgumentException(
                    "Either a trusted issuer JWKS or trust material identity provider aliases are required");
        }
        String walletScheme = getConfig().get(WALLET_SCHEME);
        if (StringUtil.isNotBlank(walletScheme)) {
            resolveWalletScheme(walletScheme);
        }
    }

    /**
     * Normalizes and validates the configured wallet scheme, returning its bare form without the
     * {@code ://} separator. The scheme is rendered as the href of the wallet button on the login
     * page, so anything a browser executes or resolves locally (for example {@code javascript} or
     * {@code data}) must be rejected, as must values that are not a single valid URI scheme.
     *
     * @throws IllegalArgumentException when the value is not a valid URI scheme or is a scheme
     *         that must not be linked from the login page
     */
    static String resolveWalletScheme(String walletScheme) {
        String scheme = walletScheme.trim();
        if (scheme.endsWith("://")) {
            scheme = scheme.substring(0, scheme.length() - 3);
        }
        if (scheme.isEmpty() || !scheme.matches(URI_SCHEME_PATTERN)) {
            throw new IllegalArgumentException("Wallet scheme '" + walletScheme + "' is not a valid URI scheme");
        }
        if (FORBIDDEN_WALLET_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Wallet scheme '" + scheme + "' is not allowed for the wallet URL");
        }
        return scheme;
    }

    public String getWalletScheme() {
        String value = getConfig().get(WALLET_SCHEME);
        return StringUtil.isNotBlank(value) ? value : DEFAULT_WALLET_SCHEME;
    }

    public void setWalletScheme(String walletScheme) {
        getConfig().put(WALLET_SCHEME, walletScheme);
    }
}
