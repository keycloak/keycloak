package org.keycloak.infinispan.compatibility;

import java.util.Map;
import org.infinispan.commons.util.Version;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.compatibility.CompatibilityMetadataProvider;
import org.keycloak.infinispan.util.InfinispanUtils;

/**
 * A {@link CompatibilityMetadataProvider} to provide metadata for the CLI options under the Caching category and
 * anything related to Infinispan.
 */
public class CachingCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    public static final String ID = "caching";

    @Override
    public Map<String, String> metadata() {
        return InfinispanUtils.isRemoteInfinispan() ?
                remoteInfinispanMetadata() :
                embeddedInfinispanMetadata();
    }

    @Override
    public String getId() {
        return ID;
    }

    private static Map<String, String> remoteInfinispanMetadata() {
        return Map.of(
                "mode", "remote",
                "persistence", Boolean.toString(MultiSiteUtils.isPersistentSessionsEnabled()),
                "version", Version.getVersion()
        );
    }

    private static Map<String, String> embeddedInfinispanMetadata() {
        return Map.of(
                "mode", "embedded",
                "persistence", Boolean.toString(Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)),
                "version", Version.getVersion()
        );
    }
}
