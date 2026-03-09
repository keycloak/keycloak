package org.keycloak.protocol.ssf.transmitter.stream.storage;

import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;

import java.util.List;

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
     * Gets all stream configurations.
     *
     * @return A list of all stream configurations
     */
    List<StreamConfig> getAllStreams();

    /**
     * Deletes a stream configuration.
     *
     * @param streamId The stream ID
     */
    void deleteStream(String streamId);
}
