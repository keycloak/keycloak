package org.keycloak.cluster.infinispan;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.CacheDecorators;
import org.keycloak.timer.ScheduledTask;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.WORK_CACHE_NAME;

/**
 * Periodically publishes the current node's timestamp to the work cache for clock skew detection.
 * Runs at a configurable interval and writes a {@link ClockSkewTimestampSample} with a TTL of 3x the interval.
 */
public class ClockSkewDetectionTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(ClockSkewDetectionTask.class);

    public static final String CLOCK_SKEW_SAMPLE_KEY_PREFIX = "clock-skew-sample::";

    private final String nodeName;
    private final long sampleTtlSeconds;

    public ClockSkewDetectionTask(String nodeName, long sampleTtlSeconds) {
        this.nodeName = Objects.requireNonNull(nodeName, "nodeName cannot be null");
        if (sampleTtlSeconds <= 0) {
            throw new IllegalArgumentException("sampleTtlSeconds must be positive, got: " + sampleTtlSeconds);
        }
        this.sampleTtlSeconds = sampleTtlSeconds;
    }

    @Override
    public void run(KeycloakSession session) {
        Cache<String, Object> workCache = session.getProvider(InfinispanConnectionProvider.class)
                .getCache(WORK_CACHE_NAME);

        long currentTimestamp = Time.currentTimeMillis();
        ClockSkewTimestampSample sample = new ClockSkewTimestampSample(nodeName, currentTimestamp);

        String key = CLOCK_SKEW_SAMPLE_KEY_PREFIX + SecretGenerator.getInstance().generateSecureID();

        if (logger.isTraceEnabled()) {
            logger.tracef("Publishing clock skew timestamp sample: node=%s, timestamp=%d, key=%s",
                    nodeName, currentTimestamp, key);
        }

        CacheDecorators.ignoreReturnValues(workCache)
                .put(key, sample, sampleTtlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public String getTaskName() {
        return "ClockSkewDetectionTask-" + nodeName;
    }
}
