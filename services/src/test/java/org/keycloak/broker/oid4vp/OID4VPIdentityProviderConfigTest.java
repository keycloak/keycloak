package org.keycloak.broker.oid4vp;

import java.util.Map;

import org.keycloak.models.IdentityProviderModel;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 *
 * @author rmartinc
 */
public class OID4VPIdentityProviderConfigTest {

    private static final String REQUESTED_CREDENTIALS = """
            {"credentials": [{"id": "pid", "format": "dc+sd-jwt", "types": ["urn:eudi:pid:1"], "trustMaterialIdps": ["trust"]}],
             "principal": {"credential": "pid", "claim": "personal_administrative_number"}}""";

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
    public void validateRequestedCredentials() {
         IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                 () -> create(Map.of()).validate(null));
         assertThat(iae.getMessage(), is("The requested credentials are required"));

         create(Map.of(OID4VPIdentityProviderConfig.REQUESTED_CREDENTIALS, REQUESTED_CREDENTIALS)).validate(null);
    }

    @Test
    public void validateThePrincipalCredentialIsAlwaysPresented() {
        String requestedCredentials = """
                {"credentials": [
                  {"id": "pid", "format": "dc+sd-jwt", "types": ["urn:eudi:pid:1"], "trustMaterialIdps": ["trust"]},
                  {"id": "employee", "format": "dc+sd-jwt", "types": ["employee"], "trustMaterialIdps": ["trust"]}],
                 "credentialSets": [{"options": [["pid", "employee"], ["pid"]]}],
                 "principal": {"credential": "employee", "claim": "sub"}}""";
        OID4VPIdentityProviderConfig config = create(Map.of(OID4VPIdentityProviderConfig.REQUESTED_CREDENTIALS, requestedCredentials));

        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> config.validate(null));
        assertThat(iae.getMessage(),
                is("The principal credential 'employee' must be part of every option of at least one required credential set"));
        assertEquals("employee", config.getRequestedCredentials().principal().credential());
    }

    private void validateWalletSchemeSuccess(String scheme) {
        create(Map.of(OID4VPIdentityProviderConfig.WALLET_SCHEME, scheme, OID4VPIdentityProviderConfig.REQUESTED_CREDENTIALS, REQUESTED_CREDENTIALS)).validate(null);
    }

    private void validateWalletSchemeFailure(String scheme) {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> create(Map.of(OID4VPIdentityProviderConfig.WALLET_SCHEME, scheme)).validate(null));
        assertThat(iae.getMessage(), is("Invalid wallet scheme '" + scheme + "'"));
    }

    private void validateResponseModeSuccess(String mode) {
        create(Map.of(OID4VPIdentityProviderConfig.RESPONSE_MODE, mode, OID4VPIdentityProviderConfig.REQUESTED_CREDENTIALS, REQUESTED_CREDENTIALS)).validate(null);
    }

    private void validateResponseModeFailure(String mode) {
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                () -> create(Map.of(OID4VPIdentityProviderConfig.RESPONSE_MODE, mode)).validate(null));
        assertThat(iae.getMessage(), is("Unsupported OID4VP response mode " + mode));
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
