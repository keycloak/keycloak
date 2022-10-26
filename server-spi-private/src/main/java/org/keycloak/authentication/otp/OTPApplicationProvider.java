package org.keycloak.authentication.otp;

import org.keycloak.models.OTPPolicy;
import org.keycloak.provider.Provider;

public interface OTPApplicationProvider extends Provider {

    String getName();

    boolean supports(OTPPolicy policy);

}
