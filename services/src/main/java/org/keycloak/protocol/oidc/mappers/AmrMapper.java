package org.keycloak.protocol.oidc.mappers;

import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;

import java.util.HashMap;
import java.util.Map;

public class AmrMapper {
    enum AmrValue {
        PWD("pwd"),
        OTP("otp"),
        MFA("mfa");

        private final String text;
        AmrValue(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    private static Map<String, String> amrMap;

    static {
        amrMap = new HashMap<>();
        amrMap.put(UsernamePasswordFormFactory.PROVIDER_ID, AmrValue.PWD.toString());
        amrMap.put(OTPFormAuthenticatorFactory.PROVIDER_ID, AmrValue.OTP.toString());
    }

    public static String getAmr(String authenticationMethod){
        return amrMap.getOrDefault(authenticationMethod, null);
    }
}
