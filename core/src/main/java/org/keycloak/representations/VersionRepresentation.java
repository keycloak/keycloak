package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonProperty;
import org.keycloak.common.Version;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VersionRepresentation {
    public static final VersionRepresentation SINGLETON;

    private final String version = Version.VERSION;
    private final String buildTime = Version.BUILD_TIME;

    static {
         SINGLETON = new VersionRepresentation();
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("build-time")
    public String getBuildTime() {
        return buildTime;
    }
}
