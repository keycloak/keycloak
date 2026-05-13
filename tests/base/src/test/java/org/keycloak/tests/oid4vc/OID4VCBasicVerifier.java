package org.keycloak.tests.oid4vc;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;

import static org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration.VERIFIABLE_CREDENTIAL_TYPE_KEY;

/**
 * A specialized Wallet facade for OID4VCP integration tests.
 *
 * @author <a href="mailto:tdiesler@proton.me">Thomas Diesler</a>
 */
public class OID4VCBasicVerifier {

    private final ManagedRealm realm;
    private final OAuthClient oauth;

    public OID4VCBasicVerifier(ManagedRealm realm, OAuthClient oauth) {
        this.realm = realm;
        this.oauth = oauth;
    }

    public OID4VCAuthorizationRequest authorizationRequest() {
        OID4VCAuthorizationRequest request = new OID4VCAuthorizationRequest(oauth);
        return request;
    }

    public String createCredentialQuery(OID4VCTestContext ctx, String credType) {
        return credType + ":Query";
    }

    /**
     * [TODO] Call the /create-credential-presentation-uri endpoint
     */
    public String createCredentialPresentationRequestUri(OID4VCTestContext ctx, String credType) {
        return credType + ":CredentialPresentationRequestUri";
    }

    /**
     * [TODO] Validate the Credential Presentation Response
     */
    public boolean validateCredentialPresentationResponse(OID4VCTestContext ctx, List<Credential> vpRes) throws VerificationException {
        if (vpRes.isEmpty() || !vpRes.get(0).isSdJwt()) {
            return false;
        }
        SdJwtVP sdJwtVP = SdJwtVP.of(String.valueOf(vpRes.get(0).getCredential()));
        JsonWebToken sdJwt = TokenVerifier.create(sdJwtVP.getIssuerSignedJWT().getJws(), JsonWebToken.class).getToken();
        Map<String, Object> otherClaims = sdJwt.getOtherClaims();
        var vct = otherClaims.get(VERIFIABLE_CREDENTIAL_TYPE_KEY);
        return ctx.getCredentialType().equals(vct);
    }

    public List<Credential> decodeVPToken(OID4VCTestContext ctx, String vpToken, Supplier<List<Credential>> decoder) {
        return decoder.get();
    }
}
