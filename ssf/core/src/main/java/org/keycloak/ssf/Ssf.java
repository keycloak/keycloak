package org.keycloak.ssf;

import org.keycloak.ssf.event.SsfEventProvider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Collection of SSF constants and entry points.
 */
public class Ssf {

    public static final String SCOPE_SSF_READ = "ssf.read";

    public static final String SCOPE_SSF_MANAGE = "ssf.manage";

    public static final String SSF_WELL_KNOWN_METADATA_PATH = ".well-known/ssf-configuration";

    public static final String SSF_REALM_RESOURCE_PATH = "ssf";

    public static final String SSF_TRANSMITTER_PATH = "transmitter";

    public static final String SSF_RECEIVERS_PATH = "receivers";

    public static final String APPLICATION_SECEVENT_JWT_TYPE = "application/secevent+jwt";

    /**
     * 4.1.1. Explicit Typing of SETs
     *
     * @see https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-4.1.1
     */
    public static final String SECEVENT_JWT_TYPE = "secevent+jwt";

    public static final String DELIVERY_METHOD_PUSH_URI = "urn:ietf:rfc:8935";

    public static final String DELIVERY_METHOD_RISC_PUSH_URI = "https://schemas.openid.net/secevent/risc/delivery-method/push";

    public static final String DELIVERY_METHOD_POLL_URI = "urn:ietf:rfc:8936";

    public static final String DELIVERY_METHOD_RISC_POLL_URI = "https://schemas.openid.net/secevent/risc/delivery-method/poll";

    private Ssf() {
    }

    public static SsfEventProvider events() {
        var session = getKeycloakSession();
        if (session == null) {
            return null;
        }
        return session.getProvider(SsfEventProvider.class);
    }

}
