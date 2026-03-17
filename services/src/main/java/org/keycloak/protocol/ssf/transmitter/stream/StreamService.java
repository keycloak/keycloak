package org.keycloak.protocol.ssf.transmitter.stream;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.SsfException;
import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.stream.StreamStatusValue;
import org.keycloak.protocol.ssf.support.SsfAuthUtil;
import org.keycloak.protocol.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadata;
import org.keycloak.protocol.ssf.transmitter.metadata.SsfTransmitterMetadataService;
import org.keycloak.protocol.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * Service for managing SSF streams.
 */
public class StreamService {

    protected static final Logger log = Logger.getLogger(StreamService.class);

    protected final SsfStreamStore streamStore;

    protected final SsfTransmitterMetadataService transmitterService;

    public StreamService(SsfStreamStore streamStore, SsfTransmitterMetadataService transmitterService) {
        this.streamStore = streamStore;
        this.transmitterService = transmitterService;
    }

    /**
     * Creates a new stream.
     *
     * @param streamConfig The stream configuration
     * @return The created stream configuration
     */
    public StreamConfig createStream(StreamConfig streamConfig) {

        validate(streamConfig);

        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        checkClient();

        streamConfig.setStreamId(createStreamId(session, streamConfig));

        // Set default status if not provided
        // should we enable stream status by default?
        streamConfig.setStatus(StreamStatusValue.enabled);

        // set issuer
        String iss = transmitterService.getTransmitterMetadata().getIssuer();
        streamConfig.setIssuer(iss);

//        // set current client as audience
        ClientModel receiverClient = session.getContext().getClient();
        String receiverClientId = receiverClient.getClientId();
//        streamConfiguration.setAudience(Set.of(receiverClientId));
        Set<String> audience = new HashSet<>(streamConfig.getAudience());

        // TODO shall we really add the audience of the stream?
        String streamAudience = transmitterService.getTransmitterMetadata().getIssuer() + "/ssf/receivers/" + receiverClientId + "/" + streamConfig.getStreamId();
        audience.add(streamAudience);
        streamConfig.setAudience(audience);

        // Requested events
        Set<String> eventsRequested = streamConfig.getEventsRequested();

        // Compute delivered events based on requested events
        streamConfig.setEventsDelivered(transmitterService.getEventsDelivered(streamConfig, eventsRequested));

        // Return supported events
        streamConfig.setEventsSupported(transmitterService.getSupportedEvents());

        // Set timestamps
        int now = Time.currentTime();
        streamConfig.setCreatedAt(now);
        streamConfig.setUpdatedAt(now);

        streamConfig.setMinVerificationInterval(Ssf.DEFAULT_MIN_VERIFICATION_INTERVAL);

        streamConfig.setStatus(StreamStatusValue.enabled);

        // use stream delivery as is

        // TODO move stream config to client via Client Stream Store
        if (SsfAuthUtil.hasScope(Ssf.SCOPE_APPLE_ABM)) {

            if (streamConfig.getProfile() == null) {
                streamConfig.setProfile(Ssf.PROFILE_SSE_CAEP);
            }

            if (streamConfig.getVerificationTrigger() == null) {
                streamConfig.setVerificationTrigger(StreamConfig.VerificationTrigger.TRANSMITTER_INITIATED);
            }

            if (streamConfig.getVerificationDelayMillis() == null) {
                streamConfig.setVerificationDelayMillis(Ssf.TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS);
            }
        }

        // Store the stream configuration
        streamStore.saveStream(streamConfig);

        if (streamConfig.getVerificationTrigger() == StreamConfig.VerificationTrigger.TRANSMITTER_INITIATED) {
            scheduleTransmitterInitiatedAsyncStreamVerification(streamConfig, session);
        }

        return streamConfig;
    }

    protected void checkClient() {
        List<StreamConfig> availableStreams = streamStore.getAvailableStreams();
        if (availableStreams != null && !availableStreams.isEmpty()) {
            throw new DuplicateStreamConfigException("Only one stream per receiver is allowed");
        }
    }

    protected String createStreamId(KeycloakSession session, StreamConfig streamConfig) {
        return UUID.randomUUID().toString();
    }

    protected void scheduleTransmitterInitiatedAsyncStreamVerification(StreamConfig streamConfig, KeycloakSession session) {
        StreamVerificationRequest verificationRequest = new StreamVerificationRequest();
        verificationRequest.setStreamId(streamConfig.getStreamId());
        // If the Verification Event is initiated by the Transmitter then this parameter MUST not be set.
        // https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.4.2-5
        // verificationRequest.setState(UUID.randomUUID().toString());

        SsfTransmitterProvider provider = session.getProvider(SsfTransmitterProvider.class);
        SsfTransmitterMetadata transmitterMetadata = provider.transmitterService().getTransmitterMetadata();

        log.debugf("Scheduling Verification request after stream creation for stream %s", streamConfig.getStreamId());

        String realmId = session.getContext().getRealm().getId();
        String clientId = session.getContext().getClient().getClientId();
        HttpRequest httpRequest = session.getContext().getHttpRequest();
        KeycloakSessionFactory keycloakSessionFactory = session.getKeycloakSessionFactory();

        var executor = Executors.newSingleThreadScheduledExecutor();
        int delay = streamConfig.getVerificationDelayMillis();
        executor.schedule(() -> {
            try (KeycloakSession subSession = keycloakSessionFactory.create()) {
                subSession.getTransactionManager().begin();
                subSession.setAttribute("ssfTransmitterMetadata", transmitterMetadata);

                RealmModel realm = subSession.realms().getRealm(realmId);
                ClientModel clientById = realm.getClientByClientId(clientId);
                subSession.getContext().setRealm(realm);
                subSession.getContext().setClient(clientById);
                subSession.getContext().setHttpRequest(httpRequest);

                KeycloakSessionUtil.setKeycloakSession(subSession);
                try {
                    log.debugf("Sending transmitter initiated Verification request after stream creation for stream %s", streamConfig.getStreamId());
                    boolean verificationSent = triggerTransmitterInitiatedStreamVerification(streamConfig, subSession, verificationRequest);
                    if (verificationSent) {
                        log.debugf("Verification transmitter initiated request sent after stream creation for stream %s", streamConfig.getStreamId());
                    }
                    subSession.getTransactionManager().commit();
                } finally {
                    KeycloakSessionUtil.setKeycloakSession(null);
                }

            } catch (Exception e) {
                log.errorf(e, "Failed to send transmitter initiated verification request after stream creation for stream %s", streamConfig.getStreamId());
            } finally {
                executor.shutdown();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    protected boolean triggerTransmitterInitiatedStreamVerification(StreamConfig streamConfig, KeycloakSession subSession, StreamVerificationRequest verificationRequest) {
        var ssfProvider = subSession.getProvider(SsfTransmitterProvider.class);
        return ssfProvider.verificationService().triggerVerification(verificationRequest);
    }

    protected void validate(StreamConfig streamConfig) {

        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery == null) {
            throw new SsfException("Invalid stream configuration: missing delivery configuration");
        }

        if (delivery.getMethod() == null) {
            throw new SsfException("Invalid stream configuration: missing delivery method");
        }

        switch (delivery.getMethod()) {
            case Ssf.DELIVERY_METHOD_PUSH_URI, Ssf.DELIVERY_METHOD_RISC_PUSH_URI -> {
                if (delivery.getEndpointUrl() == null) {
                    throw new SsfException("Invalid stream configuration: missing delivery endpoint push URL");
                }
            }
            case Ssf.DELIVERY_METHOD_POLL_URI, Ssf.DELIVERY_METHOD_RISC_POLL_URI -> {
                if (delivery.getEndpointUrl() != null) {
                    if (!delivery.getEndpointUrl().startsWith(transmitterService.getTransmitterMetadata().getIssuer())) {
                        throw new SsfException("Invalid stream configuration: delivery endpoint poll URL is defined by transmitter");
                    }
                }
                throw new SsfException("Invalid stream configuration: unsupported delivery method");
            }
            default -> throw new SsfException("Invalid stream configuration: unsupported delivery method");
        }
    }

    /**
     * Gets a stream by ID.
     *
     * @param streamId The stream ID
     * @return The stream configuration, or null if not found
     */
    public StreamConfig getStream(String streamId) {
        return streamStore.getStream(streamId);
    }

    /**
     * Gets all streams for the current client context.
     *
     * @return A list of all stream configurations
     */
    public List<StreamConfig> getAvailableStreams() {
        return streamStore.getAvailableStreams();
    }

    /**
     * Gets all enabled streams across all clients in the realm.
     *
     * @return A list of all enabled stream configurations
     */
    public List<StreamConfig> findAllEnabledStreams() {
        return streamStore.findAllEnabledStreams();
    }

    /**
     * Finds a stream by ID across all clients in the realm.
     *
     * @param streamId The stream ID
     * @return The stream configuration, or null if not found
     */
    public StreamConfig findStreamById(String streamId) {
        return streamStore.findStreamById(streamId);
    }

    /**
     * Updates a stream.
     *
     * @param streamConfig The updated stream configuration
     * @return The updated stream configuration, or null if not found
     */
    public StreamConfig updateStream(StreamConfig streamConfig) {

        String streamId = streamConfig.getStreamId();
        StreamConfig existingStream = streamStore.getStream(streamId);

        if (existingStream == null) {
            return null;
        }

        validate(streamConfig);

        // Update timestamp
        existingStream.setUpdatedAt(Time.currentTime());
        existingStream.setDescription(streamConfig.getDescription());

        // set events requested
        Set<String> eventsRequested = streamConfig.getEventsRequested();
        existingStream.setEventsRequested(eventsRequested);

        // set events delivered
        Set<String> eventsDelivered = new HashSet<>(eventsRequested);
        eventsDelivered.retainAll(transmitterService.getSupportedEvents());
        existingStream.setEventsDelivered(eventsDelivered);


        // Store the updated stream configuration
        streamStore.saveStream(existingStream);

        return existingStream;
    }


    public StreamConfig replaceStream(StreamConfig streamConfig) {

        String streamId = streamConfig.getStreamId();
        StreamConfig existingStream = streamStore.getStream(streamId);

        if (existingStream == null) {
            return null;
        }

        validate(streamConfig);

        // Update timestamp
        existingStream.setUpdatedAt(Time.currentTime());
        existingStream.setDescription(streamConfig.getDescription());

        // set events requested
        Set<String> eventsRequested = streamConfig.getEventsRequested();
        existingStream.setEventsRequested(eventsRequested);

        // set events delivered
        Set<String> eventsDelivered = new HashSet<>(eventsRequested);
        eventsDelivered.retainAll(transmitterService.getSupportedEvents());
        existingStream.setEventsDelivered(eventsDelivered);


        // Store the updated stream configuration
        streamStore.saveStream(existingStream);

        return existingStream;
    }

    /**
     * Deletes a stream.
     *
     * @param streamId The stream ID
     * @return true if the stream was deleted, false if not found
     */
    public boolean deleteStream(String streamId) {
        StreamConfig existingStream = streamStore.getStream(streamId);

        if (existingStream == null) {
            return false;
        }

        streamStore.deleteStream(streamId);
        return true;
    }

    /**
     * Gets the status of a stream.
     *
     * @param streamId The stream ID
     * @return The stream status, or null if not found
     */
    public StreamStatus getStreamStatus(String streamId) {
        StreamConfig stream = streamStore.getStream(streamId);

        if (stream == null) {
            return null;
        }

        StreamStatus status = new StreamStatus();
        status.setStreamId(streamId);
        status.setStatus(stream.getStatus().getStatusCode());
        status.setReason(stream.getStatusReason());

        return status;
    }

    /**
     * Updates the status of a stream.
     *
     * @param newStreamStatus The updated stream status
     * @return The updated stream status, or null if not found
     */
    public StreamStatus updateStreamStatus(StreamStatus newStreamStatus) {

        if (newStreamStatus == null) {
            return null;
        }

        String streamId = newStreamStatus.getStreamId();
        if (streamId == null) {
            return null;
        }

        StreamConfig stream = streamStore.getStream(streamId);
        if (stream == null) {
            return null;
        }

        StreamStatus currentStreamStatus = streamStore.getStreamStatus(streamId);
        if (Objects.equals(currentStreamStatus.getStatus(), newStreamStatus.getStatus())) {
            // return current stream status
            return currentStreamStatus;
        }

        // TODO check if new status is allowed

        // Update the stream status
        streamStore.updateStreamStatus(streamId, newStreamStatus);
        stream.setUpdatedAt(Time.currentTime());

        // Update stream status
        return streamStore.updateStreamStatus(streamId, newStreamStatus);
    }
}
