package org.keycloak.sdjwt.sdjwtvp;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.OKPPublicJWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.rule.CryptoInitRule;
import org.keycloak.sdjwt.DisclosureSpec;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.IssuerSignedJwtVerificationOpts;
import org.keycloak.sdjwt.SdJwt;
import org.keycloak.sdjwt.SdJwtUtils;
import org.keycloak.sdjwt.TestSettings;
import org.keycloak.sdjwt.TestUtils;
import org.keycloak.sdjwt.vp.KeyBindingJWT;
import org.keycloak.sdjwt.vp.KeyBindingJwtVerificationOpts;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.KeyWrapperUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_EXP;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_IAT;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_ISSUER;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_JWK;
import static org.keycloak.OID4VCConstants.CLAIM_NAME_NBF;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP256R1;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP384R1;
import static org.keycloak.common.crypto.CryptoConstants.EC_KEY_SECP521R1;
import static org.keycloak.sdjwt.sdjwtvp.SdJwtVPVerificationTest.testSettings;

import static org.hamcrest.CoreMatchers.is;

/**
 * Test of various algorithms and scenarios for SD-JWT key binding
 */
public abstract class SdJwtKeyBindingTest {

    @ClassRule
    public static CryptoInitRule cryptoInitRule = new CryptoInitRule();

    @Test
    public void testEdDSAKeyBindingWithEd25519() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateEddsaKeyPair(Algorithm.Ed25519),
                keyPair -> JWKBuilder.create().okp(keyPair.getPublic()),
                jwk -> assertEdDSAKey(jwk, Algorithm.Ed25519));
    }

    @Test
    public void testEdDSAKeyBindingWithEd448() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateEddsaKeyPair(Algorithm.Ed448),
                keyPair -> JWKBuilder.create().okp(keyPair.getPublic()),
                jwk -> assertEdDSAKey(jwk, Algorithm.Ed448));
    }

    @Test
    public void testEc256KeyBinding() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP256R1),
                keyPair -> JWKBuilder.create().ec(keyPair.getPublic()),
                jwk -> assertEcKey(jwk, "P-256"));
    }

    @Test
    public void testEc384KeyBinding() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP384R1),
                keyPair -> JWKBuilder.create().ec(keyPair.getPublic()),
                jwk -> assertEcKey(jwk, "P-384"));
    }

    @Test
    public void testEc521KeyBinding() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateEcKeyPair(EC_KEY_SECP521R1),
                keyPair -> JWKBuilder.create().ec(keyPair.getPublic()),
                jwk -> assertEcKey(jwk, "P-521"));
    }

    @Test
    public void testRSA2048KeyBinding() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateRsaKeyPair(2048),
                keyPair -> JWKBuilder.create().rsa(keyPair.getPublic()),
                jwk -> assertRsaKey(jwk));
    }

    @Test
    public void testRSA4096KeyBinding() throws VerificationException {
        testKeyBinding(() -> KeyUtils.generateRsaKeyPair(4096),
                keyPair -> JWKBuilder.create().rsa(keyPair.getPublic()),
                jwk -> assertRsaKey(jwk));
    }

    private void testKeyBinding(Supplier<KeyPair> keyPairSupplier, Function<KeyPair, JWK> jwkProvider, Consumer<JWK> keyFormatValidator) throws VerificationException {
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("family_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("email", "6Ij7tM-a5iVPGboS5tmvVA")
                .build();

        // Read claims provided by the holder
        ObjectNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-holder-claims.json");

        int currentTime = Time.currentTime();
        holderClaimSet.put(CLAIM_NAME_ISSUER, "https://example.com/issuer");
        holderClaimSet.put(CLAIM_NAME_IAT, currentTime);
        holderClaimSet.put(CLAIM_NAME_NBF, currentTime);
        holderClaimSet.put(CLAIM_NAME_EXP, currentTime + 60);

        // Generate key-binding key pair
        KeyPair keyPair = keyPairSupplier.get();
        JWK publicJwk = jwkProvider.apply(keyPair);

        KeyWrapper keyWrapper = JWKSUtils.getKeyWrapper(publicJwk);
        keyWrapper.setPrivateKey(keyPair.getPrivate());

        KeyBindingJWT keyBindingJWT = generateKeyBindingJWT(publicJwk.getKeyId());

        // Create issuer-signed JWT with the key attached
        IssuerSignedJWT issuerSignedJWT = IssuerSignedJWT.builder()
                .withClaims(holderClaimSet, disclosureSpec)
                .withKeyBindingKey(publicJwk)
                .build();
        SdJwt sdJwt = SdJwt.builder()
                .withIssuerSignedJwt(issuerSignedJWT)
                .withKeybindingJwt(keyBindingJWT)
                .withIssuerSigningContext(TestSettings.getInstance().getIssuerSignerContext())
                .withKeyBindingSigningContext(KeyWrapperUtil.createSignatureSignerContext(keyWrapper))
                .build();

        String sdJwtString = sdJwt.toString();

        // 2 - Parse presentation and verify successfully (especially key binding)

        // Just use the presentation with all the claims disclosed as provided by issuer
        SdJwtVP sdJwtVP = SdJwtVP.of(sdJwtString);

        // Expect correct JWK
        JsonNode cnf = sdJwtVP.getCnfClaim();
        Assert.assertNotNull(cnf);
        JWK jwk = SdJwtUtils.mapper.convertValue(cnf.get(CLAIM_NAME_JWK), JWK.class);

        keyFormatValidator.accept(jwk);

        sdJwtVP.verify(
                defaultIssuerVerifyingKeys(),
                defaultIssuerSignedJwtVerificationOpts().build(),
                defaultKeyBindingJwtVerificationOpts().build()
        );

        // 3 - Test incorrect key-binding signature
        KeyBindingJWT invalidBindingJWT = generateKeyBindingJWT(publicJwk.getKeyId());
        invalidBindingJWT.getPayload().put("nonce", "invalid");
        String invalidSdJwt = SdJwt.builder()
                .withIssuerSignedJwt(issuerSignedJWT)
                .withKeybindingJwt(invalidBindingJWT)
                .withIssuerSigningContext(TestSettings.getInstance().getIssuerSignerContext())
                .withKeyBindingSigningContext(KeyWrapperUtil.createSignatureSignerContext(keyWrapper))
                .build()
                .toString();

        // Replace signature with the signature from valid sdJwt
        String signature1 = sdJwtString.substring(sdJwtString.lastIndexOf('.') + 1);
        invalidSdJwt = invalidSdJwt.substring(0, invalidSdJwt.lastIndexOf('.') + 1);
        invalidSdJwt = invalidSdJwt + signature1;

        SdJwtVP invalidSdJwtVP = SdJwtVP.of(invalidSdJwt);
        try {
            invalidSdJwtVP.verify(
                    defaultIssuerVerifyingKeys(),
                    defaultIssuerSignedJwtVerificationOpts().build(),
                    defaultKeyBindingJwtVerificationOpts().build()
            );
            Assert.fail("Not expected to successfully validate key-binding JWT");
        } catch (VerificationException ve) {
            Assert.assertEquals("Key binding JWT invalid", ve.getMessage());
        }
    }

    private void assertEdDSAKey(JWK jwk, String expectedCurve) {
        Assert.assertEquals(2, jwk.getOtherClaims().size());
        Assert.assertEquals(expectedCurve, jwk.getOtherClaims().get(OKPPublicJWK.CRV));
        Assert.assertThat(jwk.getOtherClaims().containsKey(OKPPublicJWK.X), is(true));
    }

    private void assertEcKey(JWK jwk, String expectedCurve) {
        Assert.assertEquals(3, jwk.getOtherClaims().size());
        Assert.assertEquals(expectedCurve, jwk.getOtherClaims().get(ECPublicJWK.CRV));
        Assert.assertThat(jwk.getOtherClaims().containsKey(ECPublicJWK.X), is(true));
        Assert.assertThat(jwk.getOtherClaims().containsKey(ECPublicJWK.Y), is(true));
    }

    private void assertRsaKey(JWK jwk) {
        Assert.assertEquals(2, jwk.getOtherClaims().size());
        Assert.assertThat(jwk.getOtherClaims().containsKey(RSAPublicJWK.PUBLIC_EXPONENT), is(true));
        Assert.assertThat(jwk.getOtherClaims().containsKey(RSAPublicJWK.MODULUS), is(true));
    }

    private KeyBindingJWT generateKeyBindingJWT(String keyId) {
        int currentTime = Time.currentTime();

        return KeyBindingJWT.builder()
                /* header */
                .withKid(keyId)
                /* body */
                .withIat(currentTime)
                .withNbf(currentTime)
                .withExp(currentTime + 60)
                .withNonce("1234567890")
                .withAudience("https://verifier.example.org")
                .build();
    }


    private List<SignatureVerifierContext> defaultIssuerVerifyingKeys() {
        return Collections.singletonList(testSettings.issuerVerifierContext);
    }

    private IssuerSignedJwtVerificationOpts.Builder defaultIssuerSignedJwtVerificationOpts() {
        return IssuerSignedJwtVerificationOpts.builder()
                .withClockSkew(0);
    }

    private KeyBindingJwtVerificationOpts.Builder defaultKeyBindingJwtVerificationOpts() {
        return KeyBindingJwtVerificationOpts.builder()
                .withKeyBindingRequired(true)
                .withNonceCheck("1234567890")
                .withAudCheck("https://verifier.example.org");
    }
}
