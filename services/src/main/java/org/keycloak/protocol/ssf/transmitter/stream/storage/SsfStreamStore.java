package org.keycloak.protocol.ssf.transmitter.stream.storage;

import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.transmitter.stream.SsfEventsConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationConfig;

/**
 * Interface for storing and retrieving SSF stream configurations.
 */
public interface SsfStreamStore {

    /**
     * Saves a stream configuration.
     *
     * @param streamConfig The stream configuration to save
     */
    void saveStream(StreamConfig streamConfig);

    /**
     * Update the stream status
     *
     * @param streamId
     * @param streamStatus
     * @return
     */
    StreamStatus updateStreamStatus(String streamId, StreamStatus streamStatus);

    /**
     * Get the stream status
     *
     * @param streamId
     * @return
     */
    StreamStatus getStreamStatus(String streamId);

    /**
     * Gets a stream configuration by ID.
     *
     * @param streamId The stream ID
     * @return The stream configuration, or null if not found
     */
    StreamConfig getStream(String streamId);

    /**
     * Gets all stream configurations for the current client context.
     *
     * @return A list of all stream configurations
     */
    List<StreamConfig> getAvailableStreams();

    /**
     * Returns every stream configuration attached to a client whose SSF
     * receiver capability is enabled (i.e. the client carries the
     * {@code ssf.enabled} attribute). This does <strong>not</strong> filter
     * by per-stream {@code StreamStatusValue} — a stream registered against
     * an SSF-enabled client is returned regardless of whether it is
     * {@code enabled}, {@code paused}, or {@code disabled}; the dispatcher
     * applies the per-stream status filter in
     * {@code SecurityEventTokenDispatcher#isStreamEnabled} before actually
     * delivering an event.
     *
     * <p>Used when there is no specific client context on the session, e.g.
     * when the event listener fans an event out to all receivers.
     */
    List<StreamConfig> findStreamsForSsfReceiverClients();

    /**
     * Finds a stream configuration by stream ID across all clients in the realm.
     * This is used when there is no specific client context.
     *
     * @param streamId The stream ID
     * @return The stream configuration, or null if not found
     */
    StreamConfig findStreamById(String streamId);

    /**
     * Deletes a stream configuration.
     *
     * @param streamId The stream ID
     */
    void deleteStream(String streamId);

    StreamVerificationConfig getStreamVerificationConfig(String streamId, ClientModel client);

    SsfEventsConfig getEventsConfig(ClientModel client, Set<String> eventsRequested);
}
