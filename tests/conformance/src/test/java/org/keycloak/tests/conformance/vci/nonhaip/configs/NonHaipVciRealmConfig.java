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

package org.keycloak.tests.conformance.vci.nonhaip.configs;

import java.util.List;

import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.tests.conformance.vci.VciConformanceRealmUtil;
import org.keycloak.tests.conformance.vci.nonhaip.VciClientKey;

import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT2;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.X509_TRUST_IDP_ALIAS;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.attesterX509TrustIdentityProvider;

/**
 * Non-HAIP variant of the OID4VCI conformance realm authenticates the conformance
 * clients with private_key_jwt (confidential clients holding a registered public JWKS).
 */
public class NonHaipVciRealmConfig implements RealmConfig {

    public static final String NON_HAIP_PLAN = "oid4vci-1_0-issuer-test-plan";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        VciConformanceRealmUtil.applyCommon(realm, false)
                .clients(nonHaipConformanceClient(CLIENT, false, VciClientKey.publicJwks(), false),
                        nonHaipConformanceClient(CLIENT2, true, VciClientKey.publicJwks2(), false),
                        VciConformanceRealmUtil.appClient())
                .update(rep -> {
                    VciConformanceRealmUtil.applyKeyProviders(rep);
                    rep.setIdentityProviders(List.of(attesterX509TrustIdentityProvider()));
                });
        return realm;
    }

    public static ClientBuilder nonHaipConformanceClient(String clientId, boolean wildcardRedirect, JsonNode publicJwks, boolean mdoc) {
        return VciConformanceRealmUtil.baseConformanceClient(clientId, wildcardRedirect, mdoc)
                .authenticatorType(JWTClientAuthenticator.PROVIDER_ID)
                .attribute(OIDCConfigAttributes.USE_JWKS_STRING, "true")
                .attribute(OIDCConfigAttributes.JWKS_STRING, publicJwks.toString())
                .attribute(OID4VCIConstants.OID4VCI_ATTESTER_TRUST_IDPS_ATTR, X509_TRUST_IDP_ALIAS) // this is required for key attestation tests
                .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, "true"); // this is required for key attestation tests
    }
}
