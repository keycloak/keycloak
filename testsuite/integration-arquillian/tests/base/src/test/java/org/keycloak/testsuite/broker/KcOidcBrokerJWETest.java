/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.broker;

import java.util.List;
import java.util.Map;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

/**
 * <p>Tests the broker using a JWE encrypted token for id token and user info. The test
 * can be extended to use different algorithms. The default uses RSA-OAEP as
 * encryption key management algorithm, A256GCM as the content encryption
 * algorithm and RS512 as the signature algorithm.</p>
 *
 * @author rmartinc
 */
public class KcOidcBrokerJWETest extends AbstractBrokerTest {

    private final String encAlg;
    private final String encEnc;
    private final String sigAlg;

    public KcOidcBrokerJWETest() {
        this(JWEConstants.RSA_OAEP, JWEConstants.A256GCM, Algorithm.RS512);
    }

    protected KcOidcBrokerJWETest(String encAlg, String encEnc, String sigAlg) {
        this.encAlg = encAlg;
        this.encEnc = encEnc;
        this.sigAlg = sigAlg;
    }

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public List<ClientRepresentation> createProviderClients() {
                List<ClientRepresentation> clientsRepList = super.createProviderClients();
                for (ClientRepresentation client : clientsRepList) {
                    Map<String, String> attrs = client.getAttributes();

                    // use the certs from the consumer realm to perform the encryption
                    attrs.put(OIDCConfigAttributes.USE_JWKS_URL, "true");
                    attrs.put(OIDCConfigAttributes.JWKS_URL, BrokerTestTools.getConsumerRoot() +
                        "/auth/realms/" + BrokerTestConstants.REALM_CONS_NAME + "/protocol/openid-connect/certs");

                    // assign the encryption and signature attributes
                    if (encAlg != null) {
                        attrs.put(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG, encAlg);
                        attrs.put(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ALG, encAlg);
                    }

                    if (encEnc != null) {
                        attrs.put(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC, encEnc);
                        attrs.put(OIDCConfigAttributes.USER_INFO_ENCRYPTED_RESPONSE_ENC, encEnc);
                    }

                    if (sigAlg != null) {
                        attrs.put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, sigAlg);
                        attrs.put(OIDCConfigAttributes.USER_INFO_RESPONSE_SIGNATURE_ALG, sigAlg);
                    }
                }
                return clientsRepList;
            }

            @Override
            public RealmRepresentation createConsumerRealm() {
                RealmRepresentation realm = super.createConsumerRealm();

                if (encAlg != null) {
                    // create the RSA component for the encryption in the specified alg
                    ComponentExportRepresentation component = new ComponentExportRepresentation();
                    component.setName("rsa-enc-generated");
                    component.setProviderId("rsa-enc-generated");

                    MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
                    config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
                    config.putSingle("keyUse", KeyUse.ENC.name());
                    config.putSingle("algorithm", encAlg);
                    component.setConfig(config);

                    MultivaluedHashMap<String, ComponentExportRepresentation> components = realm.getComponents();
                    if (components == null) {
                        components = new MultivaluedHashMap<>();
                        realm.setComponents(components);
                    }
                    components.add(KeyProvider.class.getName(), component);
                }

                return realm;
            }
        };
    }
}
