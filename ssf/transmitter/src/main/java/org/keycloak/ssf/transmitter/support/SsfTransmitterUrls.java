package org.keycloak.ssf.transmitter.support;

import org.keycloak.ssf.Ssf;

public final class SsfTransmitterUrls {

    public static final String SSF_TRANSMITTER_BASE_PATH_SUFFIX = "%s/%s".formatted(Ssf.SSF_REALM_RESOURCE_PATH, Ssf.SSF_TRANSMITTER_PATH);

    private SsfTransmitterUrls() {
    }

    public static String getSsfTransmitterBasePath(String issuerUrl) {
        return issuerUrl + "/" + SSF_TRANSMITTER_BASE_PATH_SUFFIX;
    }

    public static String getStreamsEndpointUrl(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/streams";
    }

    public static String getStreamStatusEndpointUrl(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/streams/status";
    }

    public static String getStreamVerificationEndpointUrl(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/verify";
    }

    public static String getAddSubjectEndpointUrl(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/subjects/add";
    }

    public static String getRemoveSubjectEndpointUrl(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/subjects/remove";
    }

    /**
     * Builds the poll endpoint URL for a given receiver client and
     * stream id. The transmitter writes this URL into
     * {@code delivery.endpoint_url} of the stream-create response per
     * SSF §6.1.2 (the spec mandates the transmitter — not the
     * receiver — owns this URL for poll delivery).
     */
    public static String getPollEndpointUrl(String issuerUrl, String clientId, String streamId) {
        return getSsfTransmitterBasePath(issuerUrl)
                + "/receivers/" + clientId
                + "/streams/" + streamId
                + "/poll";
    }
}
