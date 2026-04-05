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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderMapperSyncMode;
import org.keycloak.models.utils.DefaultKeyProviders;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static java.util.Optional.ofNullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

    protected ComponentExportRepresentation getConsumerKeyProvider() {
        // create the RSA component for the encryption in the specified alg
        ComponentExportRepresentation component = new ComponentExportRepresentation();
        component.setName("rsa-enc-generated");
        component.setProviderId("rsa-enc-generated");

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.putSingle("priority", DefaultKeyProviders.DEFAULT_PRIORITY);
        config.putSingle("keyUse", KeyUse.ENC.name());
        config.putSingle("algorithm", encAlg);
        component.setConfig(config);
        return component;
    }

    protected ComponentExportRepresentation getProviderKeyProvider() {
        return null;
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
                    ComponentExportRepresentation component = getConsumerKeyProvider();
                    if (component != null) {
                        MultivaluedHashMap<String, ComponentExportRepresentation> components = realm.getComponents();
                        if (components == null) {
                            components = new MultivaluedHashMap<>();
                            realm.setComponents(components);
                        }
                        components.add(KeyProvider.class.getName(), component);
                    }
                }

                return realm;
            }
            
            @Override
            public RealmRepresentation createProviderRealm() {
                RealmRepresentation realm = super.createProviderRealm();

                if (sigAlg != null) {
                    ComponentExportRepresentation component = getProviderKeyProvider();
                    if (component != null) {
                        MultivaluedHashMap<String, ComponentExportRepresentation> components = realm.getComponents();
                        if (components == null) {
                            components = new MultivaluedHashMap<>();
                            realm.setComponents(components);
                        }
                        components.add(KeyProvider.class.getName(), component);
                    }
                }

                return realm;
            }
        };
    }

    @Test
    public void testIdentityClaimsFromUserInfoEndpoint() {
        configureUserInfoEndpointMappers();
        testLogInAsUserInIDP();
        UsersResource users = realmsResouce().realm(bc.consumerRealmName()).users();
        List<UserRepresentation> usersRep = users.search(bc.getUserLogin(), true);
        assertFalse(usersRep.isEmpty());
        UserRepresentation userRep = usersRep.get(0);
        List<String> expectedAttribute = ofNullable(userRep.getAttributes()).orElse(Map.of()).getOrDefault("user-info", List.of());
        assertFalse(expectedAttribute.isEmpty());
        assertEquals("true", expectedAttribute.get(0));
    }

    private void configureUserInfoEndpointMappers() {
        RealmResource providerRealm = realmsResouce().realm(bc.providerRealmName());
        ClientRepresentation client = providerRealm.clients().findByClientId(bc.getIDPClientIdInProviderRealm()).get(0);

        ProtocolMapperRepresentation claimMapper = new ProtocolMapperRepresentation();
        claimMapper.setName("custom-claim-hardcoded-mapper");
        claimMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        claimMapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "user-info");
        config.put(HardcodedClaim.CLAIM_VALUE, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE, "false");
        claimMapper.setConfig(config);
        ClientResource clientResource = providerRealm.clients().get(client.getId());
        ProtocolMappersResource protocolMappers = clientResource.getProtocolMappers();
        List<ProtocolMapperRepresentation> mappers = protocolMappers.getMappersPerProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        ProtocolMapperRepresentation mapper = mappers.stream().filter(new Predicate<ProtocolMapperRepresentation>() {
            @Override
            public boolean test(ProtocolMapperRepresentation mapper) {
                return UserPropertyMapper.PROVIDER_ID.equals(mapper.getProtocolMapper())
                        && mapper.getConfig().getOrDefault(ProtocolMapperUtils.USER_ATTRIBUTE, "").equals("email");
            }
        }).findAny().orElse(null);
        clientResource.getProtocolMappers().delete(mapper.getId());
        client.getProtocolMappers().add(claimMapper);
        clientResource.update(client);

        IdentityProviderResource idp = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
        IdentityProviderMapperRepresentation attributeMapper = new IdentityProviderMapperRepresentation();
        attributeMapper.setName("attribute-mapper");
        attributeMapper.setIdentityProviderMapper(UserAttributeMapper.PROVIDER_ID);
        attributeMapper.setIdentityProviderAlias(bc.getIDPAlias());
        attributeMapper.setConfig(ImmutableMap.<String,String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderMapperSyncMode.INHERIT.toString())
                .put(UserAttributeMapper.CLAIM, "user-info")
                .put(UserAttributeMapper.USER_ATTRIBUTE, "user-info")
                .build());
        idp.addMapper(attributeMapper);
    }
}
