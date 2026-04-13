package org.keycloak.protocol.ssf.transmitter.stream.storage;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ssf.stream.StreamStatus;
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
     * Gets all enabled stream configurations across all clients in the realm.
     * This is used when there is no specific client context, e.g., when dispatching events from the event listener.
     *
     * @return A list of all enabled stream configurations
     */
    List<StreamConfig> findAllEnabledStreams();

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
}
