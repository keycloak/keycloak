/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sdjwt.consumer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.TestSettings;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;

import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public abstract class SdJwtPresentationConsumerTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    SdJwtPresentationConsumer sdJwtPresentationConsumer = new SdJwtPresentationConsumer();
    static TestSettings testSettings = TestSettings.getInstance();

    @Test
    public void shouldVerifySdJwtPresentation() throws VerificationException {
        sdJwtPresentationConsumer.verifySdJwtPresentation(
                exampleSdJwtVP(),
                examplePresentationRequirements(),
                exampleTrustedSdJwtIssuers(),
                defaultIssuerSignedJwtVerificationOpts(),
                defaultKeyBindingJwtVerificationOpts()
        );
    }

    @Test
    public void shouldFail_IfPresentationRequirementsNotMet() {
        SimplePresentationDefinition definition = SimplePresentationDefinition.builder()
                .addClaimRequirement("vct", ".*diploma.*")
                .build();

        VerificationException exception = assertThrows(VerificationException.class,
                () -> sdJwtPresentationConsumer.verifySdJwtPresentation(
                        exampleSdJwtVP(),
                        definition,
                        exampleTrustedSdJwtIssuers(),
                        defaultIssuerSignedJwtVerificationOpts(),
                        defaultKeyBindingJwtVerificationOpts()
                )
        );

        assertTrue(exception.getMessage()
                .contains("A required field was not presented: `vct`"));
    }

    private SdJwtVP exampleSdJwtVP() {
        String sdJwtVPString = TestUtils.readFileAsString(getClass(), "sdjwt/s20.1-sdjwt+kb.txt");
        return SdJwtVP.of(sdJwtVPString);
    }

    private PresentationRequirements examplePresentationRequirements() {
        return SimplePresentationDefinition.builder()
                .addClaimRequirement("sub", "\"user_[0-9]+\"")
                .addClaimRequirement("given_name", ".*")
                .build();
    }

    private List<TrustedSdJwtIssuer> exampleTrustedSdJwtIssuers() {
        return Arrays.asList(
                new StaticTrustedSdJwtIssuer(
                        Collections.singletonList(testSettings.holderVerifierContext)
                ),
                new StaticTrustedSdJwtIssuer(
                        Collections.singletonList(testSettings.issuerVerifierContext)
                )
        );
    }

    private IssuerSignedJwtVerificationOpts defaultIssuerSignedJwtVerificationOpts() {
        return IssuerSignedJwtVerificationOpts.builder()
                .withRequireIssuedAtClaim(false)
                .withRequireNotBeforeClaim(false)
                .build();
    }

    private KeyBindingJwtVerificationOpts defaultKeyBindingJwtVerificationOpts() {
        return KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withAllowedMaxAge(Integer.MAX_VALUE)
                .withNonce("1234567890")
                .withAud("https://verifier.example.org")
                .withRequireExpirationClaim(false)
                .withRequireNotBeforeClaim(false)
                .build();
    }
}
