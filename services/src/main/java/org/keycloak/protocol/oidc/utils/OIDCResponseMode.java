package org.keycloak.protocol.oidc.utils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum OIDCResponseMode {

    QUERY, FRAGMENT, FORM_POST;

    public static OIDCResponseMode parse(String responseMode, OIDCResponseType responseType) {
        if (responseMode == null) {
            return getDefaultResponseMode(responseType);
        } else {
            return Enum.valueOf(OIDCResponseMode.class, responseMode.toUpperCase());
        }
    }

    private static OIDCResponseMode getDefaultResponseMode(OIDCResponseType responseType) {
        if (responseType.isImplicitOrHybridFlow()) {
            return OIDCResponseMode.FRAGMENT;
        } else {
            return OIDCResponseMode.QUERY;
        }
    }
}
