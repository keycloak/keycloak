package org.keycloak.ssf.transmitter.support;

import org.keycloak.ssf.Ssf;

public final class SsfTransmitterUrls {

    public static final String SSF_TRANSMITTER_BASE_PATH_SUFFIX = "%s/%s".formatted(Ssf.SSF_REALM_RESOURCE_PATH, Ssf.SSF_TRANSMITTER_PATH);

    private SsfTransmitterUrls() {
    }

    public static String getSsfTransmitterBasePath(String issuerUrl) {
        return issuerUrl + "/" + SSF_TRANSMITTER_BASE_PATH_SUFFIX;
    }

    public static String streamsEndpoint(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/streams";
    }

    public static String streamStatusEndpoint(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/streams/status";
    }

    public static String streamVerificationEndpoint(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/verify";
    }

    public static String addSubjectEndpoint(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/subjects/add";
    }

    public static String removeSubjectEndpoint(String issuerUrl) {
        return getSsfTransmitterBasePath(issuerUrl) + "/subjects/remove";
    }
}
