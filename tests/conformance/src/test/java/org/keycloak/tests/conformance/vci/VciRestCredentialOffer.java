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

package org.keycloak.tests.conformance.vci;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.RealmBuilder;

import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CREDENTIAL_CONFIGURATION_ID;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.HOLDER;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.PASSWORD;

/**
 * Creates a credential offer through the REST API like an issuer application. The holder gets an access token from
 * the issuer application client and uses it to request the offer. The offer is not bound to that client, so the
 * wallet redeems it with its own client.
 */
public final class VciRestCredentialOffer {

    private VciRestCredentialOffer() {
    }

    public static RealmBuilder allowHolderToCreateOffers(RealmBuilder realm) {
        return realm.realmRoles(CREDENTIAL_OFFER_CREATE.getName())
                .update(rep -> rep.getUsers().stream()
                        .filter(user -> HOLDER.equals(user.getUsername()))
                        .forEach(user -> {
                            List<String> roles = new ArrayList<>(user.getRealmRoles());
                            roles.add(CREDENTIAL_OFFER_CREATE.getName());
                            user.setRealmRoles(roles);
                        }));
    }

    public static String createOfferUri(OAuthClient oauth) {
        String token = oauth.doPasswordGrantRequest(HOLDER, PASSWORD).getAccessToken();
        return oauth.oid4vc()
                .credentialOfferUriRequest(CREDENTIAL_CONFIGURATION_ID)
                .bearerToken(token)
                .send()
                .getCredentialOfferURI()
                .getCredentialOfferUri();
    }

    public static class IssuerAppClient implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("oid4vci-issuer-app")
                    .publicClient(true)
                    .serviceAccountsEnabled(false)
                    .directAccessGrantsEnabled(true)
                    .defaultClientScopes("basic", "profile", "roles")
                    .attribute(OID4VCI_ENABLED_ATTRIBUTE_KEY, "true");
        }
    }
}
