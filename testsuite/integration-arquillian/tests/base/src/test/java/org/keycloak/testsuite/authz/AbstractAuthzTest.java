package org.keycloak.testsuite.authz;

import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;

import org.junit.Before;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.testsuite.AbstractKeycloakTest;

/**
 * @author mhajas
 */
public abstract class AbstractAuthzTest extends AbstractKeycloakTest {

    @Before
    public void onBefore() {
        enableFeature(UPLOAD_SCRIPTS);
    }

    protected AccessToken toAccessToken(String rpt) {
        AccessToken accessToken;

        try {
            accessToken = new JWSInput(rpt).readJsonContent(AccessToken.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }
        return accessToken;
    }
}
