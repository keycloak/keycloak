package org.keycloak.broker.oid4vp;

import java.util.Map;

import org.keycloak.models.IdentityProviderModel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author rmartinc
 */
public class OID4VPIdentityProviderConfigTest {

    @Test
    public void validateWalletScheme() {
        validateWalletSchemeFailure("javascript");
        validateWalletSchemeFailure("JAVASCRIPT://");
        validateWalletSchemeFailure("DATA");
        validateWalletSchemeFailure("data://");
        validateWalletSchemeFailure("invalid scheme#");
        validateWalletSchemeFailure("invalid_scheme://");
        validateWalletSchemeFailure("http://localhost:8085/authorize?clientid=something");
        validateWalletSchemeFailure("https://eudi-test.dev/authorize#client_id=something");
        validateWalletSchemeFailure("https:opaque");
        validateWalletSchemeSuccess(OID4VPIdentityProviderConfig.DEFAULT_WALLET_SCHEME);
        validateWalletSchemeSuccess("openid4vp");
        validateWalletSchemeSuccess("haip://");
        validateWalletSchemeSuccess("haip");
        validateWalletSchemeSuccess("git+ssh");
        validateWalletSchemeSuccess("http://localhost:8085/authorize");

    }

    @Test
    public void validateResponseMode() {
        validateResponseModeSuccess(ResponseMode.DIRECT_POST.value());
        validateResponseModeSuccess(ResponseMode.DIRECT_POST_JWT.value());
        validateResponseModeFailure("other");
    }

    @Test
    public void validateTrustedMaterial() {
         IllegalArgumentException iae = Assert.assertThrows(IllegalArgumentException.class, () -> create(Map.of()).validate(null));
         MatcherAssert.assertThat(iae.getMessage(), Matchers.is("Either a trusted issuer JWKS or trust material identity provider aliases are required"));

         create(Map.of(OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, "{\"keys\":[]}")).validate(null);
         create(Map.of(OID4VPIdentityProviderConfig.TRUST_MATERIAL_IDPS, "other")).validate(null);
    }

    private void validateWalletSchemeSuccess(String scheme) {
        create(Map.of(OID4VPIdentityProviderConfig.WALLET_SCHEME, scheme, OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, "{\"keys\":[]}")).validate(null);
    }

    private void validateWalletSchemeFailure(String scheme) {
        IllegalArgumentException iae = Assert.assertThrows(IllegalArgumentException.class,
                () -> create(Map.of(OID4VPIdentityProviderConfig.WALLET_SCHEME, scheme)).validate(null));
        MatcherAssert.assertThat(iae.getMessage(), Matchers.is("Invalid wallet scheme '" + scheme + "'"));
    }

    private void validateResponseModeSuccess(String mode) {
        create(Map.of(OID4VPIdentityProviderConfig.RESPONSE_MODE, mode, OID4VPIdentityProviderConfig.TRUSTED_ISSUER_JWKS, "{\"keys\":[]}")).validate(null);
    }

    private void validateResponseModeFailure(String mode) {
        IllegalArgumentException iae = Assert.assertThrows(IllegalArgumentException.class,
                () -> create(Map.of(OID4VPIdentityProviderConfig.RESPONSE_MODE, mode)).validate(null));
        MatcherAssert.assertThat(iae.getMessage(), Matchers.is("Unsupported OID4VP response mode " + mode));
    }

    private OID4VPIdentityProviderConfig create(Map<String, String> config) {
        IdentityProviderModel model = new IdentityProviderModel();
        model.setAlias("oidc4vp");
        model.setProviderId(OID4VPIdentityProviderFactory.PROVIDER_ID);
        model.setEnabled(Boolean.TRUE);
        model.setConfig(config);
        return new OID4VPIdentityProviderConfig(model);
    }
}
