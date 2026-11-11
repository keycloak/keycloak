package org.keycloak.tests.model;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.persistence.EntityManager;

import org.keycloak.authentication.authenticators.browser.WebAuthnMetadataService;
import org.keycloak.common.util.Base64Url;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialPublicKeyConverter;
import org.keycloak.credential.WebAuthnCredentialModelInput;
import org.keycloak.credential.WebAuthnCredentialModelInput.KeycloakWebAuthnAuthenticationParameters;
import org.keycloak.credential.WebAuthnCredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.providers.federation.UserMapStorageFactory;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.util.JsonSerialization;

import com.webauthn4j.WebAuthnAuthenticationManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.attestation.authenticator.EC2COSEKey;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;

import static com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier.ES256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
@DatabaseTest
public class CredentialConcurrencyTest {

    private static final String SECRET = "12345678901234567890";
    private static final int LOOK_AHEAD = 3;
    private static final byte[] WEBAUTHN_ID = "credential-id".getBytes(UTF_8);

    @TestOnServer
    public void concurrentHotpReuse(KeycloakSession session) throws Exception {
        forEachStorage(session, context -> {
            for (int counter : List.of(0, LOOK_AHEAD + 1)) {
                CredentialInput input = hotpInput(context, counter);
                List<Boolean> results = validateConcurrently(session, context, input, input);
                assertEquals(1L, results.stream().filter(Boolean::booleanValue).count());

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                    UserModel user = loadUser(current, context);
                    assertCounter(user, context, counter + 1);
                    assertFalse(user.credentialManager().isValid(input));
                });
            }

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                UserModel user = loadUser(current, context);
                assertTrue(user.credentialManager().isValid(hotpInput(context, LOOK_AHEAD + 2)));
            });
        });
    }

    @TestOnServer
    public void concurrentHotpLookAheadCodes(KeycloakSession session) throws Exception {
        forEachStorage(session, context -> {
            List<Boolean> results = validateConcurrently(session, context, hotpInput(context, 0), hotpInput(context, LOOK_AHEAD));
            assertEquals(1L, results.stream().filter(Boolean::booleanValue).count());

            int expectedCounter = results.get(0) ? 1 : LOOK_AHEAD + 1;
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                UserModel user = loadUser(current, context);
                assertCounter(user, context, expectedCounter);
                for (int counter = 0; counter < expectedCounter; counter++) {
                    assertFalse(user.credentialManager().isValid(hotpInput(context, counter)));
                }
                assertTrue(user.credentialManager().isValid(hotpInput(context, expectedCounter)));
            });
        });
    }

    @TestOnServer
    public void hotpSequentialValidationAndRollback(KeycloakSession session) throws Exception {
        forEachStorage(session, context -> {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                UserModel user = loadUser(current, context);
                assertTrue(user.credentialManager().isValid(hotpInput(context, 0)));
                assertTrue(user.credentialManager().isValid(hotpInput(context, 1)));
                assertCounter(user, context, 2);
                current.getTransactionManager().setRollbackOnly();
            });

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                UserModel user = loadUser(current, context);
                assertCounter(user, context, 0);
                assertTrue(user.credentialManager().isValid(hotpInput(context, 0)));
                assertCounter(user, context, 1);
            });
        });
    }

    @TestOnServer
    public void concurrentRecoveryCodeReuse(KeycloakSession session) throws Exception {
        List<String> codes = List.of("0000000000000000", "1111111111111111", "2222222222222222");
        forEachStorage(session, realm -> RecoveryAuthnCodesCredentialModel.createFromValues(codes, 0L, "recovery codes"), context -> {
            CredentialInput input = UserCredentialModel.buildFromBackupAuthnCode(codes.get(0));
            List<Boolean> results = validateConcurrently(session, context, input, input);
            assertEquals(1L, results.stream().filter(Boolean::booleanValue).count());

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                UserModel user = loadUser(current, context);
                assertFalse(user.credentialManager().isValid(input));
                assertTrue(user.credentialManager().isValid(UserCredentialModel.buildFromBackupAuthnCode(codes.get(1))));
                RecoveryAuthnCodesCredentialModel credential = RecoveryAuthnCodesCredentialModel.createFromCredentialModel(
                        user.credentialManager().getStoredCredentialById(context.credentialId()));
                assertEquals(3, credential.getNextRecoveryAuthnCode().orElseThrow().getNumber());
            });
        });
    }

    @TestOnServer
    public void webAuthnCounterUpdatesPreserveValidatedSnapshot(KeycloakSession session) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = generator.generateKeyPair();
        ObjectConverter converter = new ObjectConverter();
        String publicKey = new CredentialPublicKeyConverter(converter)
                .convertToDatabaseColumn(EC2COSEKey.create((ECPublicKey) keyPair.getPublic(), ES256));

        for (String type : List.of(WebAuthnCredentialModel.TYPE_TWOFACTOR, WebAuthnCredentialModel.TYPE_PASSWORDLESS)) {
            WebAuthnCredentialModelInput zero = webAuthnInput(type, keyPair, 0);
            WebAuthnCredentialModelInput first = webAuthnInput(type, keyPair, 1);
            WebAuthnCredentialModelInput stale = webAuthnInput(type, keyPair, 2);
            WebAuthnCredentialModelInput winner = webAuthnInput(type, keyPair, 3);
            WebAuthnCredentialModelInput next = webAuthnInput(type, keyPair, 4);

            forEachStorage(session, realm -> WebAuthnCredentialModel.create(type, "authenticator",
                    "00000000-0000-0000-0000-000000000000", Base64.getEncoder().encodeToString(WEBAUTHN_ID),
                    null, publicKey, 0, "none"), context -> {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                    UserModel user = loadUser(current, context);
                    assertTrue(user.credentialManager().isValid(zero));
                    assertTrue(user.credentialManager().isValid(zero));
                    assertWebAuthnCounter(user, context, 0);
                });

                List<Boolean> results = validateConcurrently(session, context, first, first);
                assertEquals(1L, results.stream().filter(Boolean::booleanValue).count());

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                    UserModel user = loadUser(current, context);
                    assertWebAuthnCounter(user, context, 1);
                    WebAuthnCredentialProvider provider = new WebAuthnCredentialProvider(current, new WebAuthnMetadataService(), converter) {
                        @Override
                        public String getType() {
                            return type;
                        }

                        @Override
                        protected WebAuthnAuthenticationManager getWebAuthnAuthenticationManager() {
                            // Advance the credential after its snapshot has been validated.
                            return new WebAuthnAuthenticationManager(List.of(authentication ->
                                    assertTrue(user.credentialManager().isValid(winner))));
                        }
                    };

                    assertFalse(provider.isValid(current.getContext().getRealm(), user, stale));
                    assertWebAuthnCounter(user, context, 3);
                    assertTrue(user.credentialManager().isValid(next));
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current ->
                        assertWebAuthnCounter(loadUser(current, context), context, 4));
            });
        }
    }

    private void forEachStorage(KeycloakSession session, CredentialTest test) throws Exception {
        forEachStorage(session, realm -> OTPCredentialModel.createFromPolicy(realm, SECRET), test);
    }

    private void forEachStorage(KeycloakSession session, Function<RealmModel, CredentialModel> credentialFactory,
                               CredentialTest test) throws Exception {
        for (boolean federated : List.of(false, true)) {
            CredentialContext context = KeycloakModelUtils.runJobInTransactionWithResult(
                    session.getKeycloakSessionFactory(), current -> {
                        RealmModel realm = new RealmManager(current).createRealm("credential-" + UUID.randomUUID());
                        current.getContext().setRealm(realm);
                        realm.setOTPPolicy(new OTPPolicy(OTPCredentialModel.HOTP, HmacOTP.HMAC_SHA1, 0, 6, LOOK_AHEAD, 30, true));
                        if (federated) {
                            ComponentModel storage = new ComponentModel();
                            storage.setName("credential-user-storage");
                            storage.setProviderId(UserMapStorageFactory.PROVIDER_ID);
                            storage.setProviderType(UserStorageProvider.class.getName());
                            storage.setParentId(realm.getId());
                            storage.getConfig().putSingle(IMPORT_ENABLED, "false");
                            realm.addComponentModel(storage);
                        }
                        UserModel user = current.users().addUser(realm, "credential-user-" + realm.getId());
                        assertEquals(!federated, StorageId.isLocalStorage(user.getId()));
                        user.setEnabled(true);
                        CredentialModel credential = user.credentialManager().createStoredCredential(
                                credentialFactory.apply(realm));
                        return new CredentialContext(realm.getId(), user.getId(), credential.getId());
                    });
            try {
                test.run(context);
            } finally {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), current -> {
                    RealmModel realm = current.realms().getRealm(context.realmId());
                    current.getContext().setRealm(realm);
                    UserModel user = current.users().getUserById(realm, context.userId());
                    if (user != null) current.users().removeUser(realm, user);
                    new RealmManager(current).removeRealm(realm);
                });
            }
        }
    }

    private UserModel loadUser(KeycloakSession session, CredentialContext context) {
        RealmModel realm = session.realms().getRealm(context.realmId());
        session.getContext().setRealm(realm);
        return session.users().getUserById(realm, context.userId());
    }

    private void preloadCredential(KeycloakSession session, UserModel user, CredentialContext context) {
        user.credentialManager().getStoredCredentialById(context.credentialId());
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        // Exercise both the user cache and an already loaded persistence context.
        if (StorageId.isLocalStorage(user.getId())) {
            em.find(CredentialEntity.class, context.credentialId());
        } else {
            em.find(FederatedUserCredentialEntity.class, context.credentialId());
        }
    }

    private CredentialInput hotpInput(CredentialContext context, int counter) {
        HmacOTP generator = new HmacOTP(6, HmacOTP.HMAC_SHA1, LOOK_AHEAD);
        return new UserCredentialModel(context.credentialId(), OTPCredentialModel.TYPE, generator.generateHOTP(SECRET, counter));
    }

    private WebAuthnCredentialModelInput webAuthnInput(String type, KeyPair keyPair, int counter) throws Exception {
        DefaultChallenge challenge = new DefaultChallenge();
        byte[] clientData = JsonSerialization.writeValueAsBytes(Map.of("type", "webauthn.get",
                "challenge", Base64Url.encode(challenge.getValue()), "origin", "https://localhost"));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] authenticatorData = ByteBuffer.allocate(37)
                .put(digest.digest("localhost".getBytes(UTF_8))).put((byte) 1).putInt(counter).array();
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(authenticatorData);
        signature.update(digest.digest(clientData));

        WebAuthnCredentialModelInput input = new WebAuthnCredentialModelInput(type);
        input.setAuthenticationRequest(new AuthenticationRequest(WEBAUTHN_ID, authenticatorData, clientData, signature.sign()));
        input.setAuthenticationParameters(new KeycloakWebAuthnAuthenticationParameters(
                new ServerProperty(new Origin("https://localhost"), "localhost", challenge), false));
        return input;
    }

    private List<Boolean> validateConcurrently(KeycloakSession session, CredentialContext context,
                                             CredentialInput... inputs) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(inputs.length);
        CyclicBarrier ready = new CyclicBarrier(inputs.length);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (CredentialInput input : inputs) {
                futures.add(executor.submit(() -> KeycloakModelUtils.runJobInTransactionWithResult(
                        session.getKeycloakSessionFactory(), current -> {
                            UserModel user = loadUser(current, context);
                            preloadCredential(current, user, context);
                            await(ready);
                            return user.credentialManager().isValid(input);
                        })));
            }
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(40, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(40, TimeUnit.SECONDS));
        }
    }

    private void assertCounter(UserModel user, CredentialContext context, int expected) {
        OTPCredentialModel credential = OTPCredentialModel.createFromCredentialModel(
                user.credentialManager().getStoredCredentialById(context.credentialId()));
        assertEquals(expected, credential.getOTPCredentialData().getCounter());
    }

    private void assertWebAuthnCounter(UserModel user, CredentialContext context, long expected) {
        WebAuthnCredentialModel credential = WebAuthnCredentialModel.createFromCredentialModel(
                user.credentialManager().getStoredCredentialById(context.credentialId()));
        assertEquals(expected, credential.getWebAuthnCredentialData().getCounter());
    }

    private void await(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record CredentialContext(String realmId, String userId, String credentialId) {
    }

    @FunctionalInterface
    private interface CredentialTest {
        void run(CredentialContext context) throws Exception;
    }
}
