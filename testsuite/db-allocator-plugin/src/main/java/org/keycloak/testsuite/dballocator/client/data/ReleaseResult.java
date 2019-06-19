package org.keycloak.testsuite.dballocator.client.data;

public class ReleaseResult {

    private final String uuid;

    private ReleaseResult(String uuid) {
        this.uuid = uuid;
    }

    public static ReleaseResult successful(String uuid) {
        return new ReleaseResult(uuid);
    }

    public String getUUID() {
        return uuid;
    }

    @Override
    public String toString() {
        return "ReleaseResult{" +
                "uuid='" + uuid + '\'' +
                '}';
    }
}
