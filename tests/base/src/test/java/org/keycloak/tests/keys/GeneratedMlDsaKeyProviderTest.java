package org.keycloak.tests.keys;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.ComponentResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.keys.Attributes;
import org.keycloak.keys.DefaultKeyManager;
import org.keycloak.keys.GeneratedMlDsaKeyProviderFactory;
import org.keycloak.keys.KeyProvider;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.KeysMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.ServerWithOAuthMlDsaEnabled;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = ServerWithOAuthMlDsaEnabled.class)
public class GeneratedMlDsaKeyProviderTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @Test
    public void fallbackKeys() {
        try {
            runOnServer.run(s -> {
                KeyWrapper activeKey = new DefaultKeyManager(s).getActiveKey(s.getContext().getRealm(), KeyUse.SIG, Algorithm.ML_DSA_65);
                assertKeyWrapper(activeKey, activeKey.getProviderId(), Algorithm.ML_DSA_65, KeyStatus.ACTIVE, -100);
            });
        } finally {
            String id = realm.admin().components().query(realm.getId(), KeyProvider.class.getName(), "fallback-" + Algorithm.ML_DSA_65).stream().findFirst().orElseThrow().getId();
            realm.admin().components().removeComponent(id);
        }
    }

    @Test
    public void testDefaults() {
        String componentId = createKey("testDefaults", null, null, null, null);

        runOnServer.run(s -> {
            KeyWrapper activeKey = new DefaultKeyManager(s).getActiveKey(s.getContext().getRealm(), KeyUse.SIG, Algorithm.ML_DSA_65);
            assertKeyWrapper(activeKey, componentId, Algorithm.ML_DSA_65, KeyStatus.ACTIVE, 0);
        });
    }

    @Test
    public void testSupportedAlgorithms() {
        final Set<String> algorithms = Set.of(Algorithm.ML_DSA_44, Algorithm.ML_DSA_65, Algorithm.ML_DSA_87);
        Map<String, String> componentIds = new HashMap<>();
        for (String alg : algorithms) {
            String componentId = createKey("testSupportedAlgorithms-" + alg, 100, true, true, alg);
            componentIds.put(alg, componentId);
        }

        runOnServer.run(s -> {
            DefaultKeyManager keyManager = new DefaultKeyManager(s);
            for (String alg : algorithms) {
                KeyWrapper activeKey = keyManager.getActiveKey(s.getContext().getRealm(), KeyUse.SIG, alg);
                assertKeyWrapper(activeKey, componentIds.get(alg), alg, KeyStatus.ACTIVE, 100);
            }
        });
    }

    @Test
    public void testUpdatingAlgNotSupported() {
        String componentId = createKey("testUpdatingAlgNotSupported", 100, true, true, Algorithm.ML_DSA_65);
        ComponentResource component = realm.admin().components().component(componentId);
        ComponentRepresentation rep = component.toRepresentation();
        rep.getConfig().putSingle(Attributes.ALGORITHM_KEY, Algorithm.ML_DSA_87);
        Assertions.assertThrows(BadRequestException.class, () -> component.update(rep));
    }

    @Test
    public void testDisabled() {
        String componentId = createKey("testDisabled", 100, false, true, Algorithm.ML_DSA_65);
        runOnServer.run(s -> {
            KeyWrapper key = new DefaultKeyManager(s).getKeysStream(s.getContext().getRealm()).filter(k -> k.getAlgorithm().equals(Algorithm.ML_DSA_65)).findFirst().orElseThrow();
            assertKeyWrapper(key, componentId, Algorithm.ML_DSA_65, KeyStatus.DISABLED, 100);
        });
    }

    @Test
    public void testPassive() {
        String componentId = createKey("testDisabled", 100, true, false, Algorithm.ML_DSA_65);
        runOnServer.run(s -> {
            KeyWrapper key = new DefaultKeyManager(s).getKeysStream(s.getContext().getRealm()).filter(k -> k.getAlgorithm().equals(Algorithm.ML_DSA_65)).findFirst().orElseThrow();
            assertKeyWrapper(key, componentId, Algorithm.ML_DSA_65, KeyStatus.PASSIVE, 100);
        });
    }

    @Test
    public void verifyJwks() {
        createKey("verifyJwks", 100, true, true, Algorithm.ML_DSA_65);

        JSONWebKeySet jsonWebKeySet = oAuthClient.keys().getRealmKeys();
        JWK jwk = Arrays.stream(jsonWebKeySet.getKeys()).filter(k -> k.getAlgorithm().equals(Algorithm.ML_DSA_65)).findFirst().orElseThrow();
        Assertions.assertEquals(KeyType.AKP, jwk.getKeyType());
        Assertions.assertEquals(Algorithm.ML_DSA_65, jwk.getAlgorithm());
        Assertions.assertEquals(KeyUse.SIG.getSpecName(), jwk.getPublicKeyUse());
        Assertions.assertNull(jwk.getX509CertificateChain());
        Assertions.assertNull(jwk.getSha1x509Thumbprint());
        Assertions.assertNull(jwk.getSha256x509Thumbprint());
        Assertions.assertNotNull(jwk.getOtherClaims().get("pub"));
    }

    @Test
    public void verifyKeyMetadata() {
        createKey("verifyKeyMetadata", 100, true, true, Algorithm.ML_DSA_65);

        List<KeysMetadataRepresentation.KeyMetadataRepresentation> keys = realm.admin().keys().getKeyMetadata().getKeys().stream()
                .filter(k -> k.getAlgorithm().equals(Algorithm.ML_DSA_65)).toList();
        Assertions.assertEquals(1, keys.size());

        KeysMetadataRepresentation.KeyMetadataRepresentation keyMetadata = keys.get(0);
        Assertions.assertEquals(Algorithm.ML_DSA_65, keyMetadata.getAlgorithm());
        Assertions.assertNotNull(keyMetadata.getPublicKey());
        Assertions.assertNotNull(keyMetadata.getProviderId());
        Assertions.assertNotNull(keyMetadata.getKid());
        Assertions.assertNull(keyMetadata.getCertificate());
        Assertions.assertEquals(KeyStatus.ACTIVE.name(), keyMetadata.getStatus());
        Assertions.assertEquals(KeyType.AKP, keyMetadata.getType());
        Assertions.assertEquals(KeyUse.SIG, keyMetadata.getUse());
    }

    @Test
    public void verifyValidKeys() {
        createKey("verifyKeyMetadata", 100, true, true, Algorithm.ML_DSA_65);
        runOnServer.run(s -> {
            KeyWrapper activeKey = new DefaultKeyManager(s).getActiveKey(s.getContext().getRealm(), KeyUse.SIG, Algorithm.ML_DSA_65);
            try {
                byte[] payload = "Hello".getBytes(StandardCharsets.UTF_8);

                Signature signer = Signature.getInstance("ML-DSA");
                signer.initSign((PrivateKey) activeKey.getPrivateKey());
                signer.update(payload);
                byte[] sign = signer.sign();

                Signature verifier = Signature.getInstance("ML-DSA");
                verifier.initVerify((PublicKey) activeKey.getPublicKey());
                verifier.update(payload);
                Assertions.assertTrue(verifier.verify(sign));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String createKey(String name, Integer priority, Boolean enabled, Boolean active, String algorithm) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(realm.getId());
        rep.setProviderId(GeneratedMlDsaKeyProviderFactory.ID);
        rep.setProviderType(KeyProvider.class.getName());

        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        if (priority != null) {
            config.putSingle(Attributes.PRIORITY_KEY, Integer.toString(priority));
        }
        if (enabled != null) {
            config.putSingle(Attributes.ENABLED_KEY, Boolean.toString(enabled));
        }
        if (active != null) {
            config.putSingle(Attributes.ACTIVE_KEY, Boolean.toString(active));
        }
        if (algorithm != null) {
            config.putSingle(Attributes.ALGORITHM_KEY, algorithm);
        }
        rep.setConfig(config);

        String componentId = ApiUtil.getCreatedId(realm.admin().components().add(rep));
        realm.cleanup().add(r -> r.components().removeComponent(componentId));
        return componentId;
    }

    private static void assertKeyWrapper(KeyWrapper key, String expectedComponentId, String expectedAlgorithm, KeyStatus expectedStatus, long expectedPriority) {
        Assertions.assertNotNull(key);
        Assertions.assertEquals(expectedComponentId, key.getProviderId());
        Assertions.assertEquals(KeyType.AKP, key.getType());
        Assertions.assertEquals(KeyUse.SIG, key.getUse());
        Assertions.assertEquals(expectedAlgorithm, key.getAlgorithm());
        Assertions.assertEquals(expectedPriority, key.getProviderPriority());
        Assertions.assertEquals(expectedStatus, key.getStatus());
        Assertions.assertNotNull(key.getPrivateKey());
        Assertions.assertNotNull(key.getPublicKey());
        Assertions.assertNull(key.getCertificate());
        Assertions.assertNull(key.getCertificateChain());
    }

}
