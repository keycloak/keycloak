package org.keycloak.tests.broker.oidc;

import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.tests.broker.AbstractKcOidcEcdhEsJweBrokerTest;

@KeycloakIntegrationTest
public class KcOidcBrokerJWEEcdhEsA256KwP521A256CbcHs512Test extends AbstractKcOidcEcdhEsJweBrokerTest {

    @Override
    protected String getCurve() {
        return "P-521";
    }

    @Override
    protected String getEncAlg() {
        return JWEConstants.ECDH_ES_A256KW;
    }

    @Override
    protected String getEncEnc() {
        return JWEConstants.A256CBC_HS512;
    }

    @Override
    protected String getSigAlg() {
        return Algorithm.ES512;
    }
}
