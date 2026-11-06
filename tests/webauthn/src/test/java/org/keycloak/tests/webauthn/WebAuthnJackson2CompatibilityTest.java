package org.keycloak.tests.webauthn;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.common.util.Base64Url;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.WebAuthnCredentialModelInput;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class WebAuthnJackson2CompatibilityTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("legacyCredentials")
    public void authenticateCredentialStoredWithJackson2(String credentialType, LegacyCredential fixture) {
        managedRealm.addUser(UserBuilder.create("legacy-passkey"));
        String userId = runOnServer.fetchString(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserByUsername(realm, "legacy-passkey");
            CredentialModel credential = new CredentialModel();
            credential.setType(credentialType);
            credential.setUserLabel(fixture.algorithm);
            credential.setCredentialData(fixture.credentialData);
            credential.setSecretData("{}");
            user.credentialManager().createStoredCredential(credential);
            return user.getId();
        });

        // Reload the unchanged legacy record in a separate transaction before authentication.
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            UserModel user = session.users().getUserById(realm, userId);
            CredentialModel stored = user.credentialManager().getStoredCredentialsByTypeStream(credentialType)
                    .findFirst().orElseThrow();
            assertEquals(fixture.credentialData, stored.getCredentialData());
            WebAuthnCredentialData original = WebAuthnCredentialModel.createFromCredentialModel(stored).getWebAuthnCredentialData();
            assertEquals(0, original.getCounter());

            WebAuthnCredentialModelInput input = new WebAuthnCredentialModelInput(credentialType);
            input.setAuthenticationRequest(new AuthenticationRequest(
                    Base64Url.decode(fixture.credentialId),
                    Base64Url.decode(fixture.authenticatorData),
                    Base64Url.decode(fixture.clientDataJSON),
                    Base64Url.decode(fixture.signature)));
            ServerProperty server = new ServerProperty(new Origin(fixture.origin), fixture.rpId, new DefaultChallenge(fixture.challenge));
            input.setAuthenticationParameters(new WebAuthnCredentialModelInput.KeycloakWebAuthnAuthenticationParameters(server, false));

            assertTrue(user.credentialManager().isValid(input));

            CredentialModel updated = user.credentialManager().getStoredCredentialById(stored.getId());
            WebAuthnCredentialData data = WebAuthnCredentialModel.createFromCredentialModel(updated).getWebAuthnCredentialData();
            assertEquals(1, data.getCounter());
            assertEquals(original.getCredentialPublicKey(), data.getCredentialPublicKey());
            assertEquals(original.getCredentialId(), data.getCredentialId());
        });
    }

    // Synthetic credentials serialized with WebAuthn4J 0.30.3.RELEASE and Jackson 2.21.7 (annotations 2.21).
    // Assertions use matching test keys; these fixtures are not exports from a running Keycloak server.
    static Stream<Arguments> legacyCredentials() throws IOException {
        try (InputStream input = WebAuthnJackson2CompatibilityTest.class.getResourceAsStream("/webauthn/jackson2-credentials.json")) {
            assertNotNull(input);
            List<LegacyCredential> fixtures = JsonSerialization.readValue(input, new TypeReference<>() {});
            return fixtures.stream().flatMap(fixture -> Stream.of(
                    WebAuthnCredentialModel.TYPE_TWOFACTOR,
                    WebAuthnCredentialModel.TYPE_PASSWORDLESS
            ).map(type -> Arguments.of(type, fixture)));
        }
    }

    public static class LegacyCredential implements Serializable {
        public String algorithm;
        public long coseAlgorithm;
        public String origin;
        public String rpId;
        public String challenge;
        public String credentialData;
        public String credentialId;
        public String clientDataJSON;
        public String authenticatorData;
        public String signature;

        @Override
        public String toString() {
            return algorithm;
        }
    }
}
