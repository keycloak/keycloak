package org.keycloak.ssf.transmitter.stream.storage;

import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.transmitter.stream.SsfEventsConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamVerificationConfig;

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
    List<StreamConfig> getAvailableStreams(ClientModel receiverClient);

    /**
     * Returns every stream configuration attached to a client whose SSF
     * receiver capability is enabled (i.e. the client carries the
     * {@code ssf.enabled} attribute). This does <strong>not</strong> filter
     * by per-stream {@code StreamStatusValue} — a stream registered against
     * an SSF-enabled client is returned regardless of whether it is
     * {@code enabled}, {@code paused}, or {@code disabled}; the dispatcher
     * applies the per-stream status filter in
     * {@code SecurityEventTokenDispatcher#dispatchEvent} before actually
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
     * Retrieves the stream configuration associated with the specified client.
     *
     * @param client The client model for which the stream configuration is being retrieved.
     * @return The stream configuration associated with the given client, or null if no associated stream configuration exists.
     */
    StreamConfig getStreamForClient(ClientModel client);

    /**
     * Delete the stream configuration associated with the specified client.
     * @param client
     * @return
     */
    boolean deleteStreamForClient(ClientModel client);

    /**
     * Deletes a stream configuration.
     *
     * @param streamId The stream ID
     */
    void deleteStream(String streamId);

    /**
     * Records the current time on the receiver client's "last verified at"
     * attribute for the stream identified by {@code streamId}. Called from
     * {@link org.keycloak.ssf.transmitter.stream.StreamVerificationService#triggerVerification}
     * so every verification path — receiver-initiated, admin-initiated, and
     * transmitter-initiated automatic post-create — records a consistent
     * timestamp without each caller having to stamp the attribute itself.
     *
     * <p>Silently no-ops if no client/stream can be resolved for
     * {@code streamId}; the verification dispatch itself is authoritative
     * for whether anything was actually sent.
     */
    void recordStreamVerification(String streamId);

    StreamVerificationConfig getStreamVerificationConfig(String streamId, ClientModel client);

    SsfEventsConfig getEventsConfig(ClientModel client, Set<String> eventsRequested);


}
