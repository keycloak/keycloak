package org.keycloak.authentication.authenticators.client.extensibility;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.client.AbstractJWTClientValidator;
import org.keycloak.authentication.authenticators.client.FederatedJWTClientValidator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClientValidatorExtensibilityTest {

    @Test
    void validatorsExposeProtectedHooksForSubclassCustomization() {
        assertNotNull(CustomJWTClientValidator.class);
        assertNotNull(CustomFederatedJWTClientValidator.class);
    }

    private abstract static class CustomJWTClientValidator extends AbstractJWTClientValidator {

        private CustomJWTClientValidator() throws Exception {
            super(null, null, null);
        }

        @Override
        protected boolean validateClientAssertionParameters() {
            return true;
        }

        @Override
        protected boolean validateClient() {
            return failure(AuthenticationFlowError.CLIENT_NOT_FOUND);
        }

        @Override
        protected boolean validateSignature() {
            return failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, Response.status(Response.Status.UNAUTHORIZED).build());
        }

        @Override
        protected String getExpectedTokenIssuer() {
            return "custom-issuer";
        }

        @Override
        protected List<String> getExpectedAudiences() {
            return List.of("custom-audience");
        }

        @Override
        protected boolean isMultipleAudienceAllowed() {
            return false;
        }

        @Override
        protected int getAllowedClockSkew() {
            return 0;
        }

        @Override
        protected int getMaximumExpirationTime() {
            return 300;
        }

        @Override
        protected boolean isReusePermitted() {
            return false;
        }

        @Override
        protected String getExpectedSignatureAlgorithm() {
            return "RS256";
        }
    }

    private abstract static class CustomFederatedJWTClientValidator extends FederatedJWTClientValidator {

        private CustomFederatedJWTClientValidator() throws Exception {
            super(null, null, "custom-issuer", 0, true);
        }

        @Override
        protected boolean validateClient() {
            return true;
        }

        @Override
        protected List<String> getExpectedAudiences() {
            return List.of("custom-audience");
        }
    }
}
