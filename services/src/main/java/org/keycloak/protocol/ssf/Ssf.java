package org.keycloak.protocol.ssf;

import org.keycloak.protocol.ssf.receiver.spi.SsfReceiverProvider;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Entry-point to lookup the SsfProvider.
 */
public class Ssf {

    public static final String SCOPE_SSF_READ = "ssf.read";

    public static final String SCOPE_SSF_MANAGE = "ssf.manage";

    /**
     * NON standard internal marker scope for Apple Business Manager compatibility.
     */
    public static final String SCOPE_APPLE_ABM = "apple-abm";

    public static final String PROFILE_STANDARD = "SSF";

    public static final String PROFILE_SSE_CAEP = "SSE_CAEP";

    public static final String APPLICATION_SECEVENT_JWT_TYPE = "application/secevent+jwt";

    /**
     * 4.1.1. Explicit Typing of SETs
     *
     * @see https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-4.1.1
     */
    public static final String SECEVENT_JWT_TYPE = "secevent+jwt";

    /**
     * An integer indicating the minimum amount of time in seconds that must pass in between verification requests. If an Event Receiver submits verification requests more frequently than this, the Event Transmitter MAY respond with a 429 status code.
     */
    public static final int DEFAULT_MIN_VERIFICATION_INTERVAL = 60;

    public static final String DELIVERY_METHOD_PUSH_URI = "urn:ietf:rfc:8935";

    public static final String DELIVERY_METHOD_RISC_PUSH_URI = "https://schemas.openid.net/secevent/risc/delivery-method/push";

    public static final String DELIVERY_METHOD_POLL_URI = "urn:ietf:rfc:8936";

    public static final String DELIVERY_METHOD_RISC_POLL_URI = "https://schemas.openid.net/secevent/risc/delivery-method/poll";

    public static final int TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS = 1500;

    private Ssf() {
    }

    public static SsfReceiverProvider receiver() {
        return getKeycloakSession().getProvider(SsfReceiverProvider.class);
    }

    public static SsfTransmitterProvider transmitter() {
        return getKeycloakSession().getProvider(SsfTransmitterProvider.class);
    }
}
