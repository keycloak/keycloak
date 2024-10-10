package org.keycloak.sdjwt.consumer;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.VerificationException;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.TestSettings;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
                .withValidateIssuedAtClaim(false)
                .withValidateNotBeforeClaim(false)
                .build();
    }

    private KeyBindingJwtVerificationOpts defaultKeyBindingJwtVerificationOpts() {
        return KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withAllowedMaxAge(Integer.MAX_VALUE)
                .withNonce("1234567890")
                .withAud("https://verifier.example.org")
                .withValidateExpirationClaim(false)
                .withValidateNotBeforeClaim(false)
                .build();
    }
}
