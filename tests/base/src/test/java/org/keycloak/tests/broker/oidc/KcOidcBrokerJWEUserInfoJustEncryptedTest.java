package org.keycloak.tests.broker.oidc;

import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcJweBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerJWEUserInfoJustEncryptedTest extends AbstractKcOidcJweBrokerTest {

    @Override
    protected String getEncAlg() {
        return JWEConstants.RSA_OAEP_256;
    }

    @Override
    protected String getEncEnc() {
        return null;
    }

    @Override
    protected String getSigAlg() {
        return null;
    }
}
