package org.keycloak.authentication.otp;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;

public class FreeOTPProvider implements OTPApplicationProviderFactory, OTPApplicationProvider {

    @Override
    public OTPApplicationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public String getId() {
        return "freeotp";
    }

    @Override
    public String getName() {
        return "totpAppFreeOTPName";
    }

    @Override
    public boolean supports(OTPPolicy policy) {
        return true;
    }

    @Override
    public void close() {
    }

}
