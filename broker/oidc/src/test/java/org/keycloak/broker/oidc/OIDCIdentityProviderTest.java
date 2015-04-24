package org.keycloak.broker.oidc;

import org.junit.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCIdentityProviderTest {

    @Test
    public void testUnsupportedKeyInput() throws Exception {
        String json = "{" +
                "\"version\":\"3.0\"," +
                "\"issuer\":\"https://server.com:443\"," +
                "\"authorization_endpoint\":\"https://server.com:443/oauth2\"," +
                "\"token_endpoint\":\"https://server.com:443/token\"," +
                "\"revocation_endpoint\":\"https://server.com:443/revoke\"," +
                "\"userinfo_endpoint\":\"https://server.com:443/userinfo\"," +
                "\"jwks_uri\":\"https://server.com:443/JWKS\"," +
                "\"scopes_supported\"[\"phone\",\"address\",\"email\",\"openid\",\"profile\"]," +
                "\"response_types_supported\":[\"code\",\"token\",\"id_token\",\"code token\",\"code id_token\",\"token id_token\",\"code token id_token\"]," +
                "\"subject_types_supported\":[\"public\"]," +
                "\"id_token_signing_alg_values_supported\":[\"HS256\",\"HS384\",\"HS512\",\"RS256\",\"RS384\",\"RS512\",\"ES256\",\"ES84\",\"ES512\"]} ";
    }
}
