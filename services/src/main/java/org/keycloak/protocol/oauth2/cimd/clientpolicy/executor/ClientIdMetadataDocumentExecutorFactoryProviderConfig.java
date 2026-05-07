package org.keycloak.protocol.oauth2.cimd.clientpolicy.executor;

import org.keycloak.Config;
import org.keycloak.protocol.oauth2.cimd.provider.PersistentClientIdMetadataDocumentProviderFactory;

/**
 * The factory provider configuration for {@link AbstractClientIdMetadataDocumentExecutorFactory} as global settings.
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientIdMetadataDocumentExecutorFactoryProviderConfig {

    private final Config.Scope config;

    /**
     * CIMD provider name
     */
    private final String cimdProviderName;

    /**
     *  Min Cache Time (sec)
     */
    private final int minCacheTime;

    /**
     * Max Cache Time (sec)
     */
    private final int maxCacheTime;

    /**
     * Upper Limit Metadata Bytes
     */
    private final long upperLimitMetadataBytes;

    /**
     * Default value for {@link #cimdProviderName} :
     */
    public static final String DEFAULT_CONFIG_CIMD_PROVIDER_NAME = PersistentClientIdMetadataDocumentProviderFactory.PROVIDER_ID;

    /**
     * Default value for {@link #minCacheTime} : 5min
     */
    public static final int DEFAULT_CONFIG_MIN_CACHE_TIME = 300;

    /**
     * Default value for {@link #maxCacheTime} : 30days
     */
    public static final int DEFAULT_CONFIG_MAX_CACHE_TIME = 259200; // 30days

    /**
     * Default value for {@link #upperLimitMetadataBytes} : 5KB
     */
    public static final long DEFAULT_CONFIG_UPPER_LIMIT_METADATA_BYTES = 5000;


    public ClientIdMetadataDocumentExecutorFactoryProviderConfig(Config.Scope config) {
        this.config = config;
        cimdProviderName = config.get(AbstractClientIdMetadataDocumentExecutorFactory.CONFIG_CIMD_PROVIDER_NAME, DEFAULT_CONFIG_CIMD_PROVIDER_NAME);
        minCacheTime = config.getInt(AbstractClientIdMetadataDocumentExecutorFactory.CONFIG_MIN_CACHE_TIME, DEFAULT_CONFIG_MIN_CACHE_TIME);
        maxCacheTime = config.getInt(AbstractClientIdMetadataDocumentExecutorFactory.CONFIG_MAX_CACHE_TIME, DEFAULT_CONFIG_MAX_CACHE_TIME);
        upperLimitMetadataBytes = config.getLong(AbstractClientIdMetadataDocumentExecutorFactory.CONFIG_UPPER_LIMIT_METADATA_BYTES, DEFAULT_CONFIG_UPPER_LIMIT_METADATA_BYTES);
    }

    public String getCimdProviderName() {
        return cimdProviderName;
    }

    public int getMinCacheTime() {
        return minCacheTime;
    }

    public int getMaxCacheTime() {
        return maxCacheTime;
    }

    public long getUpperLimitMetadataBytes() {
        return upperLimitMetadataBytes;
    }
}
