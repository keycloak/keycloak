/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.webauthn.registration;

import com.beust.jcommander.internal.Lists;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import org.junit.Test;
import org.keycloak.models.credential.dto.WebAuthnCredentialData;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.utils.WebAuthnDataWrapper;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.crypto.Algorithm.ES256;
import static org.keycloak.crypto.Algorithm.ES512;
import static org.keycloak.crypto.Algorithm.RS256;
import static org.keycloak.crypto.Algorithm.RS512;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class PubKeySignRegisterTest extends AbstractWebAuthnVirtualTest {

    @Test
    public void publicKeySignaturesWrong() {
        assertPublicKeyAlgorithms(false, null, Lists.newArrayList(RS512, ES512));
    }

    @Test
    public void publicKeySignaturesAlternatives() {
        assertPublicKeyAlgorithms(true, COSEAlgorithmIdentifier.ES256, Lists.newArrayList(ES256, ES512));
    }

    @Test
    public void publicKeySignaturesCorrect() {
        assertPublicKeyAlgorithms(true, COSEAlgorithmIdentifier.ES256, Collections.singletonList(ES256));
    }

    @Test
    public void publicKeySignaturesRSA() {
        assertPublicKeyAlgorithms(false, null, Lists.newArrayList(RS256, ES512));
    }

    @Test
    public void publicKeySignaturesEmpty() {
        assertPublicKeyAlgorithms(true, COSEAlgorithmIdentifier.ES256, Collections.emptyList());
    }

    @Test
    public void publicKeySignaturesNonExisting() {
        assertPublicKeyAlgorithms(true, COSEAlgorithmIdentifier.ES256, Collections.singletonList("RSSSS2048"));
    }

    private void assertPublicKeyAlgorithms(boolean shouldSuccess, COSEAlgorithmIdentifier selectedAlgorithm, List<String> algorithms) {
        assertThat(algorithms, notNullValue());

        try (Closeable u = getWebAuthnRealmUpdater()
                .setWebAuthnPolicySignatureAlgorithms(algorithms)
                .update()) {

            if (!algorithms.isEmpty()) {
                WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
                assertThat(realmData.getSignatureAlgorithms(), is(algorithms));
            }

            registerDefaultUser(shouldSuccess);

            assertThat(webAuthnErrorPage.isCurrent(), is(!shouldSuccess));
            if (!shouldSuccess) {
                assertThat(webAuthnErrorPage.getError(), containsString("The operation either timed out or was not allowed"));
                return;
            }

            final String credentialType = getCredentialType();

            getTestingClient().server(TEST_REALM_NAME).run(session -> {
                final WebAuthnDataWrapper dataWrapper = new WebAuthnDataWrapper(session, USERNAME, credentialType);
                assertThat(dataWrapper, notNullValue());

                final WebAuthnCredentialData data = dataWrapper.getWebAuthnData();
                assertThat(data, notNullValue());

                final COSEKey pubKey = dataWrapper.getKey();
                assertThat(pubKey, notNullValue());
                assertThat(pubKey.getAlgorithm(), notNullValue());
                assertThat(pubKey.getAlgorithm().getValue(), is(selectedAlgorithm.getValue()));
                assertThat(pubKey.hasPublicKey(), is(true));
            });
        } catch (IOException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
