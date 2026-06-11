package org.keycloak.authentication.authenticators.client;

import org.keycloak.authentication.ClientAuthenticationFlowContext;

public class JWTClientSecretValidator extends JWTClientValidator {

    public JWTClientSecretValidator(ClientAuthenticationFlowContext context, SignatureValidator signatureValidator, String clientAuthenticatorProviderId) throws Exception {
        super(context, signatureValidator, clientAuthenticatorProviderId);
    }

    @Override
    protected boolean isSymmetricAlgorithmAllowed() {
        return true;
    }
}
