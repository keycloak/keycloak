package org.keycloak.cluster.infinispan;

import org.keycloak.marshalling.Marshalling;

import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * Timestamp sample published by a node to the work cache, so that nodes of a newer version can detect clock skew in the cluster.
 * <p>
 * This version of Keycloak never publishes or evaluates such samples.
 * The class is only registered in the marshalling schema, so that a node does not fail when it receives a sample from a newer node
 * in a mixed-version cluster, for example during a rolling update.
 *
 * @param nodeName the name of the node that published this timestamp
 * @param timestampMillis the timestamp in milliseconds since epoch (UTC)
 */
@ProtoTypeId(Marshalling.CLOCK_SKEW_TIMESTAMP_SAMPLE)
@Proto
public record ClockSkewTimestampSample(@ProtoField(1) String nodeName, @ProtoField(2) long timestampMillis) {
}
