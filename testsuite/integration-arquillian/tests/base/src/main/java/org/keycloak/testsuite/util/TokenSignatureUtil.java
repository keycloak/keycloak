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
package org.keycloak.testsuite.util;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.keys.GeneratedEcdsaKeyProviderFactory;
import org.keycloak.keys.GeneratedRsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.TestContext;

public class TokenSignatureUtil {
    private static Logger log = Logger.getLogger(TokenSignatureUtil.class);

    private static final String TEST_REALM_NAME = "test";

    public static void changeRealmTokenSignatureProvider(Keycloak adminClient, String toSigAlgName) {
        changeRealmTokenSignatureProvider(TEST_REALM_NAME, adminClient, toSigAlgName);
    }

    public static void changeRealmTokenSignatureProvider(String realm, Keycloak adminClient, String toSigAlgName) {
        RealmRepresentation rep = adminClient.realm(realm).toRepresentation();
        log.tracef("change realm test signature algorithm from %s to %s", rep.getDefaultSignatureAlgorithm(), toSigAlgName);
        rep.setDefaultSignatureAlgorithm(toSigAlgName);
        adminClient.realm(realm).update(rep);
    }

    public static void changeClientAccessTokenSignatureProvider(ClientResource clientResource, String toSigAlgName) {
        ClientRepresentation clientRep = clientResource.toRepresentation();
        log.tracef("change client %s access token signature algorithm from %s to %s", clientRep.getClientId(), clientRep.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG), toSigAlgName);
        clientRep.getAttributes().put(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG, toSigAlgName);
        clientResource.update(clientRep);
    }

    public static void changeClientIdTokenSignatureProvider(ClientResource clientResource, String toSigAlgName) {
        ClientRepresentation clientRep = clientResource.toRepresentation();
        log.tracef("change client %s access token signature algorithm from %s to %s", clientRep.getClientId(), clientRep.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG), toSigAlgName);
        clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_SIGNED_RESPONSE_ALG, toSigAlgName);
        clientResource.update(clientRep);
    }

    public static void changeClientIdTokenEncryptionAlgProvider(ClientResource clientResource, String toAlgName) {
        ClientRepresentation clientRep = clientResource.toRepresentation();
        log.tracef("change client %s id token encryption alg algorithm from %s to %s", clientRep.getClientId(), clientRep.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG), toAlgName);
        clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ALG, toAlgName);
        clientResource.update(clientRep);
    }

    public static void changeClientIdTokenEncryptionEncProvider(ClientResource clientResource, String toEncName) {
        ClientRepresentation clientRep = clientResource.toRepresentation();
        log.tracef("change client %s id token encryption enc algorithm from %s to %s", clientRep.getClientId(), clientRep.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC), toEncName);
        clientRep.getAttributes().put(OIDCConfigAttributes.ID_TOKEN_ENCRYPTED_RESPONSE_ENC, toEncName);
        clientResource.update(clientRep);
    }

    public static boolean verifySignature(String sigAlgName, String token, Keycloak adminClient) throws Exception {
        PublicKey publicKey = getRealmPublicKey(TEST_REALM_NAME, sigAlgName, adminClient);
        JWSInput jws = new JWSInput(token);
        Signature verifier = getSignature(sigAlgName);
        verifier.initVerify(publicKey);
        verifier.update(jws.getEncodedSignatureInput().getBytes("UTF-8"));
        return verifier.verify(jws.getSignature());
    }

    public static void registerKeyProvider(String jwaAlgorithmName, Keycloak adminClient, TestContext testContext) {
        registerKeyProvider(TEST_REALM_NAME, jwaAlgorithmName, adminClient, testContext);
    }

    public static void registerKeyProvider(String realm, String jwaAlgorithmName, Keycloak adminClient, TestContext testContext) {
        switch(jwaAlgorithmName) {
            case Algorithm.RS256:
            case Algorithm.RS384:
            case Algorithm.RS512:
            case Algorithm.PS256:
            case Algorithm.PS384:
            case Algorithm.PS512:
                registerKeyProvider(realm, "algorithm", jwaAlgorithmName, GeneratedRsaKeyProviderFactory.ID, adminClient, testContext);
                break;
            case Algorithm.ES256:
            case Algorithm.ES384:
            case Algorithm.ES512:
                registerKeyProvider(realm, "ecdsaEllipticCurveKey", convertAlgorithmToECDomainParamNistRep(jwaAlgorithmName), GeneratedEcdsaKeyProviderFactory.ID, adminClient, testContext);
                break;
        }
    }

    public static String convertAlgorithmToECDomainParamNistRep(String algorithm) {
        switch(algorithm) {
            case Algorithm.ES256 :
                return "P-256";
            case Algorithm.ES384 :
                return "P-384";
            case Algorithm.ES512 :
                return "P-521";
            default :
                return null;
        }
    }

    private static void registerKeyProvider(String realm, String providerSpecificKey, String providerSpecificValue, String providerId, Keycloak adminClient, TestContext testContext) {
        long priority = System.currentTimeMillis();

        ComponentRepresentation rep = createKeyRep("valid", providerId, adminClient);
        rep.setConfig(new MultivaluedHashMap<>());
        rep.getConfig().putSingle("priority", Long.toString(priority));
        rep.getConfig().putSingle(providerSpecificKey, providerSpecificValue);

        try (Response response = adminClient.realm(realm).components().add(rep)) {
            String id = ApiUtil.getCreatedId(response);
            testContext.getOrCreateCleanup(realm).addComponentId(id);
        }
    }

    private static ComponentRepresentation createKeyRep(String name, String providerId, Keycloak adminClient) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(adminClient.realm(TEST_REALM_NAME).toRepresentation().getId());
        rep.setProviderId(providerId);
        rep.setProviderType(KeyProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

    private static PublicKey getRealmPublicKey(String realm, String sigAlgName, Keycloak adminClient) {
        KeysMetadataRepresentation keyMetadata = adminClient.realms().realm(realm).keys().getKeyMetadata();
        String activeKid = keyMetadata.getActive().get(sigAlgName);
        PublicKey publicKey = null;
        for (KeysMetadataRepresentation.KeyMetadataRepresentation rep : keyMetadata.getKeys()) {
            if (rep.getKid().equals(activeKid)) {
                X509EncodedKeySpec publicKeySpec = null;
                try {
                    publicKeySpec = new X509EncodedKeySpec(Base64.decode(rep.getPublicKey()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                KeyFactory kf = null;
                try {
                    kf = KeyFactory.getInstance(rep.getType());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                try {
                    publicKey = kf.generatePublic(publicKeySpec);
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
        }
        return publicKey;
    }

    private static Signature getSignature(String sigAlgName) {
        try {
            // use Bouncy Castle for signature verification intentionally
            Signature signature = Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(sigAlgName), BouncyIntegration.PROVIDER);
            return signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
