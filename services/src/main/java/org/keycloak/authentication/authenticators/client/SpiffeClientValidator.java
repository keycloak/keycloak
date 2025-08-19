package org.keycloak.authentication.authenticators.client;

import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.ClientModel;

public class SpiffeClientValidator extends JWTClientValidator {

    public SpiffeClientValidator(ClientAuthenticationFlowContext context, String clientAuthenticatorProviderId) {
        super(context, clientAuthenticatorProviderId);
    }

    @Override
    protected boolean isJwtIdRequired() {
        return false;
    }

    @Override
    protected boolean isIssuerRequired() {
        return false;
    }

    @Override
    public boolean validateClient() {
        if (!super.validateClient()) {
            return false;
        }

        String subjectPrefix = "spiffe://" + client.getAttribute(SpiffeClientAuthenticator.TRUST_DOMAIN_KEY) + "/";
        if (!token.getSubject().startsWith(subjectPrefix)) {
            throw new RuntimeException("Subject is not a SPIFFE ID, or not associated with the correct domain");
        }
        return true;
    }

    protected boolean supportedSubject(String subject) {
        return subject.startsWith("spiffe://");
    }

    @Override
    protected boolean validateSubject(ClientModel client) {
        String subjectPrefix = "spiffe://" + client.getAttribute(SpiffeClientAuthenticator.TRUST_DOMAIN_KEY) + "/";
        if (!token.getSubject().startsWith(subjectPrefix)) {
            throw new RuntimeException("Subject is not a SPIFFE ID, or not associated with the correct domain");
        }
        return true;
    }
}
