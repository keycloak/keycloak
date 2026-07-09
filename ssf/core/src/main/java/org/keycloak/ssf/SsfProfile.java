package org.keycloak.ssf;

public enum SsfProfile {

    /**
     * Standard SSF 1.0, as defined by https://openid.net/specs/openid-sharedsignals-framework-1_0-final.html
     */
    SSF_1_0,

    /**
     * Legacy SSE CAEP Profile, as defined by https://openid.net/specs/openid-sse-framework-1_0.html
     *
     * This is required to support compatibility with Apple Business Manager / Apple School Manager.
     */
    SSE_CAEP

}
