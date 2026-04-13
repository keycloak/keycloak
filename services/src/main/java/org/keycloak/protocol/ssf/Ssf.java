package org.keycloak.protocol.ssf;

import org.keycloak.protocol.ssf.event.SsfEventProvider;
import org.keycloak.protocol.ssf.receiver.spi.SsfReceiverProvider;
import org.keycloak.protocol.ssf.transmitter.SsfScopes;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;

import static org.keycloak.utils.KeycloakSessionUtil.getKeycloakSession;

/**
 * Collection of SSF constants and entry points.
 */
public class Ssf {

    public static final String SCOPE_SSF_READ = SsfScopes.SCOPE_SSF_READ;

    public static final String SCOPE_SSF_MANAGE = SsfScopes.SCOPE_SSF_MANAGE;

    public static final String SSF_WELL_KNOWN_METADATA_PATH = ".well-known/ssf-configuration";

    public static final String SSF_REALM_RESOURCE_PATH = "ssf";

    public static final String SSF_TRANSMITTER_PATH = "transmitter";

    public static final String SSF_RECEIVERS_PATH = "receivers";

    public static final String SSF_TRANSMITTER_BASE_PATH_SUFFIX = "%s/%s".formatted(SSF_REALM_RESOURCE_PATH, SSF_TRANSMITTER_PATH);

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

    private Ssf() {
    }

    public static SsfReceiverProvider receiver() {
        return getKeycloakSession().getProvider(SsfReceiverProvider.class);
    }

    public static SsfTransmitterProvider transmitter() {
        return getKeycloakSession().getProvider(SsfTransmitterProvider.class);
    }

    public static SsfEventProvider events() {
        return getKeycloakSession().getProvider(SsfEventProvider.class);
    }

    public static String getSsfTransmitterBasePath(String issuerUrl) {
        return issuerUrl + "/" + Ssf.SSF_TRANSMITTER_BASE_PATH_SUFFIX;
    }

    public static String streamsEndpoint(String issuerUrl) {
        return Ssf.getSsfTransmitterBasePath(issuerUrl) + "/streams";
    }

    public static String streamStatusEndpoint(String issuerUrl) {
        return Ssf.getSsfTransmitterBasePath(issuerUrl) + "/streams/status";
    }

    public static String streamVerificationEndpoint(String issuerUrl) {
        return Ssf.getSsfTransmitterBasePath(issuerUrl) + "/verify";
    }

}
