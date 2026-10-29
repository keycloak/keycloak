package org.keycloak.cluster.infinispan;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Timestamp sample from a cluster node used for clock skew detection.
 * Each node periodically publishes its current timestamp to the work cache,
 * allowing other nodes to detect if clocks are significantly ahead.
 * <p>
 * This supports the cluster-wide clock skew detection mechanism defined in issue #54284.
 * Nodes compare these samples against their local clocks to warn operators when NTP
 * configuration may be incorrect or when clock drift exceeds acceptable thresholds.
 *
 * @param nodeName the name of the node that published this timestamp
 * @param timestampMillis the timestamp in milliseconds since epoch (UTC)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.CLOCK_SKEW_TIMESTAMP_SAMPLE)
@Proto
public record ClockSkewTimestampSample(String nodeName, long timestampMillis) {
}
