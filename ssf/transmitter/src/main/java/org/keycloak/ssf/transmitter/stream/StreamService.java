package org.keycloak.ssf.transmitter.stream;

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
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfException;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.metadata.TransmitterMetadata;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.event.SsfSignatureAlgorithms;
import org.keycloak.ssf.transmitter.event.SsfUserSubjectFormats;
import org.keycloak.ssf.transmitter.metadata.TransmitterMetadataService;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.stream.storage.client.ClientStreamStore;
import org.keycloak.utils.KeycloakSessionUtil;

import org.jboss.logging.Logger;

/**
 * Service for managing SSF streams.
 */
public class StreamService {

    protected static final Logger log = Logger.getLogger(StreamService.class);

    /**
     * Length caps for receiver-supplied fields on {@code /streams}
     * POST/PATCH/PUT. Chosen so that even a maximally-populated stream fits
     * inside the client attribute value column with a safety margin — see
     * {@link ClientStreamStore#MAX_STREAM_CONFIG_JSON_BYTES} for the overall
     * total-size guard. Violations produce {@link SsfException} → HTTP 400.
     */
    protected static final int MAX_DESCRIPTION_LENGTH = 255;
    protected static final int MAX_EVENTS_REQUESTED_COUNT = 32;
    protected static final int MAX_EVENT_TYPE_LENGTH = 256;
    protected static final int MAX_DELIVERY_ENDPOINT_URL_LENGTH = 512;
    protected static final int MAX_DELIVERY_AUTHORIZATION_HEADER_LENGTH = 1024;
    protected static final int MAX_DELIVERY_ADDITIONAL_PARAMETERS_COUNT = 8;
    protected static final int MAX_DELIVERY_ADDITIONAL_PARAMETER_KEY_LENGTH = 64;
    protected static final int MAX_DELIVERY_ADDITIONAL_PARAMETER_VALUE_LENGTH = 256;

    protected final KeycloakSession session;

    protected final SsfStreamStore streamStore;

    protected final TransmitterMetadataService transmitterService;

    public StreamService(KeycloakSession session, SsfStreamStore streamStore, TransmitterMetadataService transmitterService) {
        this.session = session;
        this.streamStore = streamStore;
        this.transmitterService = transmitterService;
    }

    /**
     * Creates a new stream from a receiver-supplied {@link StreamConfigInputRepresentation}
     * request body (SSF spec §8.1.1.2).
     *
     * <p>Transmitter-controlled fields ({@code stream_id}, {@code iss},
     * {@code aud}, {@code events_supported}, {@code events_delivered}, the
     * {@code kc_*} extensions, …) are computed here and are intentionally
     * absent from {@link StreamConfigUpdateRepresentation}/{@link StreamConfigInputRepresentation} so a
     * receiver cannot supply them over the wire: Jackson rejects unknown
     * fields with 400 at bind time.
     *
     * @param input The receiver-supplied create request.
     * @return The created stream configuration.
     */
    public StreamConfig createStream(StreamConfigInputRepresentation input) {

        checkClient();

        StreamConfig streamConfig = new StreamConfig();
        // Receiver-writable fields first so validate() sees the delivery
        // configuration the receiver actually supplied.
        replaceReceiverFields(input, streamConfig);

        validate(streamConfig);

        // Transmitter-supplied identity fields.
        streamConfig.setStreamId(createStreamId(session, streamConfig));
        streamConfig.setStatus(StreamStatusValue.enabled);
        streamConfig.setIssuer(transmitterService.getTransmitterMetadata().getIssuer());

        ClientModel receiverClient = session.getContext().getClient();
        streamConfig.setAudience(createAudience(streamConfig, receiverClient));

        Set<String> eventsRequested = streamConfig.getEventsRequested();

        // Compute delivered events based on requested events
        SsfEventsConfig eventsConfig = streamStore.getEventsConfig(receiverClient, eventsRequested);
        streamConfig.setEventsDelivered(eventsConfig.eventsDelivered());
        // Return supported events
        streamConfig.setEventsSupported(eventsConfig.eventsSupported());

        // Set timestamps
        int now = Time.currentTime();
        streamConfig.setCreatedAt(now);
        streamConfig.setUpdatedAt(now);

        streamConfig.setMinVerificationInterval(SsfTransmitter.current().getConfig().getMinVerificationIntervalSeconds());

        applySignatureAlgorithmFromClient(streamConfig, receiverClient);
        applyUserSubjectFormatFromClient(streamConfig, receiverClient);

        streamConfig.setStatus(StreamStatusValue.enabled);

        // Store the stream configuration
        streamStore.saveStream(streamConfig);

        log.debugf("Stream created. realm=%s client=%s streamId=%s",
                session.getContext().getRealm().getName(), session.getContext().getClient().getClientId(), streamConfig.getStreamId());

        StreamVerificationConfig streamVerificationConfig = streamStore.getStreamVerificationConfig(streamConfig.getStreamId(), session.getContext().getClient());
        if (streamVerificationConfig.verificationTrigger() == VerificationTrigger.TRANSMITTER_INITIATED) {
            scheduleTransmitterInitiatedAsyncStreamVerification(streamConfig, streamVerificationConfig, session);
        }

        return streamConfig;
    }

    protected Set<String> createAudience(StreamConfig streamConfig, ClientModel receiverClient) {
        Set<String> audience = new HashSet<>();
        String ssfClientAudience = receiverClient.getAttribute(ClientStreamStore.SSF_STREAM_AUDIENCE_KEY);
        if (ssfClientAudience != null) {
            audience.add(ssfClientAudience);
        } else {
            String streamAudience = receiverClient.getClientId() + "/" + streamConfig.getStreamId();
            audience.add(streamAudience);
        }
        return audience;
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

    protected void scheduleTransmitterInitiatedAsyncStreamVerification(StreamConfig streamConfig, StreamVerificationConfig streamVerificationConfig, KeycloakSession session) {
        StreamVerificationRequest verificationRequest = new StreamVerificationRequest();
        verificationRequest.setStreamId(streamConfig.getStreamId());
        // If the Verification Event is initiated by the Transmitter then this parameter MUST not be set.
        // https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.4.2-5
        // verificationRequest.setState(UUID.randomUUID().toString());

        SsfTransmitterProvider provider = session.getProvider(SsfTransmitterProvider.class);
        TransmitterMetadata transmitterMetadata = provider.metadataService().getTransmitterMetadata();

        log.debugf("Scheduling Verification request after stream creation for stream %s", streamConfig.getStreamId());

        String realmId = session.getContext().getRealm().getId();
        String clientId = session.getContext().getClient().getClientId();
        HttpRequest httpRequest = session.getContext().getHttpRequest();
        KeycloakSessionFactory keycloakSessionFactory = session.getKeycloakSessionFactory();

        var executor = Executors.newSingleThreadScheduledExecutor();
        int delay = streamVerificationConfig.verificationDelayMillis();
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

    /**
     * Applies the receiver-writable fields of {@code input} to
     * {@code target} using §8.1.1.3 merge semantics: only non-null fields
     * are copied; null fields leave the corresponding value on
     * {@code target} untouched.
     *
     * <p>Java beans collapse "field absent from the body" and "field present
     * and explicitly null" into the same state, so we define a null in a
     * PATCH body as "don't change this". To explicitly clear a
     * receiver-writable field a receiver must use PUT (full replace) instead.
     */
    protected void mergeReceiverFields(StreamConfigInputRepresentation input, StreamConfig target) {
        if (input.getDescription() != null) {
            target.setDescription(input.getDescription());
        }
        if (input.getEventsRequested() != null) {
            target.setEventsRequested(input.getEventsRequested());
        }
        if (input.getDelivery() != null) {
            target.setDelivery(input.getDelivery());
        }
    }

    /**
     * Applies the receiver-writable fields of {@code input} to
     * {@code target} using replace semantics: all receiver-writable fields
     * are unconditionally copied, so absent fields on {@code input} reset
     * the corresponding value on {@code target} to {@code null}.
     *
     * <p>Transmitter-controlled fields on {@code target} ({@code stream_id},
     * {@code iss}, {@code aud}, {@code events_supported}, the {@code kc_*}
     * extensions, …) are left untouched — this method only replaces the
     * receiver-writable subset. Used for both {@code POST /streams} (apply
     * onto a fresh {@link StreamConfig}) and {@code PUT /streams} (apply
     * onto the stored {@link StreamConfig}).
     */
    protected void replaceReceiverFields(StreamConfigInputRepresentation input, StreamConfig target) {
        target.setDescription(input.getDescription());
        target.setEventsRequested(input.getEventsRequested());
        target.setDelivery(input.getDelivery());
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

        validateFieldLengths(streamConfig);
    }

    /**
     * Enforces per-field length caps on receiver-supplied fields so oversized
     * inputs are rejected with a clean {@link SsfException} → HTTP 400 rather
     * than leaking through to a DB column overflow when the stream is
     * persisted. The overall persisted-blob size is additionally capped in
     * {@link ClientStreamStore#storeStreamConfig}.
     */
    protected void validateFieldLengths(StreamConfig streamConfig) {

        String description = streamConfig.getDescription();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new SsfException("Invalid stream configuration: description exceeds "
                    + MAX_DESCRIPTION_LENGTH + " characters");
        }

        Set<String> eventsRequested = streamConfig.getEventsRequested();
        if (eventsRequested != null) {
            if (eventsRequested.size() > MAX_EVENTS_REQUESTED_COUNT) {
                throw new SsfException("Invalid stream configuration: events_requested exceeds "
                        + MAX_EVENTS_REQUESTED_COUNT + " entries");
            }
            for (String eventType : eventsRequested) {
                if (eventType != null && eventType.length() > MAX_EVENT_TYPE_LENGTH) {
                    throw new SsfException("Invalid stream configuration: events_requested entry exceeds "
                            + MAX_EVENT_TYPE_LENGTH + " characters");
                }
            }
        }

        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery != null) {
            String endpointUrl = delivery.getEndpointUrl();
            if (endpointUrl != null && endpointUrl.length() > MAX_DELIVERY_ENDPOINT_URL_LENGTH) {
                throw new SsfException("Invalid stream configuration: delivery.endpoint_url exceeds "
                        + MAX_DELIVERY_ENDPOINT_URL_LENGTH + " characters");
            }

            String authorizationHeader = delivery.getAuthorizationHeader();
            if (authorizationHeader != null && authorizationHeader.length() > MAX_DELIVERY_AUTHORIZATION_HEADER_LENGTH) {
                throw new SsfException("Invalid stream configuration: delivery.authorization_header exceeds "
                        + MAX_DELIVERY_AUTHORIZATION_HEADER_LENGTH + " characters");
            }

            java.util.Map<String, Object> additionalParameters = delivery.getAdditionalParameters();
            if (additionalParameters != null) {
                if (additionalParameters.size() > MAX_DELIVERY_ADDITIONAL_PARAMETERS_COUNT) {
                    throw new SsfException("Invalid stream configuration: delivery.additional_parameters exceeds "
                            + MAX_DELIVERY_ADDITIONAL_PARAMETERS_COUNT + " entries");
                }
                for (java.util.Map.Entry<String, Object> entry : additionalParameters.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().length() > MAX_DELIVERY_ADDITIONAL_PARAMETER_KEY_LENGTH) {
                        throw new SsfException("Invalid stream configuration: delivery.additional_parameters key exceeds "
                                + MAX_DELIVERY_ADDITIONAL_PARAMETER_KEY_LENGTH + " characters");
                    }
                    Object value = entry.getValue();
                    if (value != null && value.toString().length() > MAX_DELIVERY_ADDITIONAL_PARAMETER_VALUE_LENGTH) {
                        throw new SsfException("Invalid stream configuration: delivery.additional_parameters value for key '"
                                + entry.getKey() + "' exceeds "
                                + MAX_DELIVERY_ADDITIONAL_PARAMETER_VALUE_LENGTH + " characters");
                    }
                }
            }
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
     * Returns every stream configuration attached to a client whose SSF
     * receiver capability is enabled. Does not filter by per-stream status
     * — the dispatcher applies {@code StreamStatusValue} gating before
     * actually delivering events. See
     * {@link SsfStreamStore#findStreamsForSsfReceiverClients()}
     * for details.
     */
    public List<StreamConfig> findStreamsForSsfReceiverClients() {
        return streamStore.findStreamsForSsfReceiverClients();
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
     * Updates a stream using SSF spec §8.1.1.3 merge semantics.
     *
     * <p>Only non-null fields on {@code update} are applied to the stored
     * stream; null/absent fields retain their current value. Transmitter-
     * controlled fields are not on {@link StreamConfigUpdateRepresentation} at all, so
     * Jackson rejects any such field in the request body with 400 at bind
     * time — the receiver cannot clobber transmitter state such as
     * {@code iss}, {@code aud}, or {@code kc_created_at} by round-tripping
     * a previously fetched representation.
     *
     * <p>Note on null vs. absent: Java beans collapse the two into the same
     * state, so we define null-in-a-PATCH-body as "don't change this". To
     * explicitly clear a receiver-writable field, use PUT (full replace).
     *
     * @return The updated stream configuration, or {@code null} if the stream
     *         identified by {@code update.streamId} does not exist.
     */
    public StreamConfig updateStream(StreamConfigUpdateRepresentation streamUpdate) {

        if (streamUpdate == null || streamUpdate.getStreamId() == null) {
            throw new SsfException("Invalid stream update: stream_id is required");
        }

        StreamConfig existingStream = streamStore.getStream(streamUpdate.getStreamId());
        if (existingStream == null) {
            return null;
        }

        boolean eventsRequestedChanged = streamUpdate.getEventsRequested() != null;

        mergeReceiverFields(streamUpdate, existingStream);

        // Re-run full validation against the merged result; delivery method /
        // push endpoint constraints are enforced on the combined state.
        validate(existingStream);

        
        ClientModel receiverClient = session.getContext().getClient();

        // Recompute events_delivered only when events_requested actually changed —
        // a pure description or delivery update should not touch the delivered set.
        if (eventsRequestedChanged) {
            SsfEventsConfig eventsConfig = streamStore.getEventsConfig(receiverClient, existingStream.getEventsRequested());
            existingStream.setEventsDelivered(eventsConfig.eventsDelivered());
        }

        applySignatureAlgorithmFromClient(existingStream, receiverClient);
        applyUserSubjectFormatFromClient(existingStream, receiverClient);

        existingStream.setUpdatedAt(Time.currentTime());

        streamStore.saveStream(existingStream);

        log.debugf("Stream updated. realm=%s client=%s streamId=%s",
                session.getContext().getRealm().getName(), session.getContext().getClient().getClientId(), streamUpdate.getStreamId());

        return existingStream;
    }

    /**
     * Replaces a stream using SSF spec §8.1.1.4 semantics.
     *
     * <p>The receiver sends the entire receiver-writable stream configuration.
     * Receiver-updatable fields on the stored stream are replaced with the
     * values from the request (including being reset to {@code null} when
     * absent). Transmitter-controlled fields ({@code iss}, {@code aud},
     * {@code events_supported}, the {@code kc_*} extensions, …) are preserved
     * from storage: they are not on {@link StreamConfigUpdateRepresentation}, so Jackson
     * rejects them in the request body with 400 at bind time.
     *
     * @return The replaced stream configuration, or {@code null} if the stream
     *         does not exist.
     */
    public StreamConfig replaceStream(StreamConfigUpdateRepresentation streamUpdate) {

        if (streamUpdate == null || streamUpdate.getStreamId() == null) {
            throw new SsfException("Invalid stream replace: stream_id is required");
        }

        StreamConfig existingStream = streamStore.getStream(streamUpdate.getStreamId());
        if (existingStream == null) {
            return null;
        }

        if (streamUpdate.getDelivery() == null) {
            throw new SsfException("Invalid stream replace: delivery is required");
        }

        // Replace receiver-updatable fields on existingStream. Omitted receiver
        // fields are reset — that is the whole point of PUT vs PATCH.
        replaceReceiverFields(streamUpdate, existingStream);

        validate(existingStream);

        
        ClientModel receiverClient = session.getContext().getClient();

        SsfEventsConfig eventsConfig = streamStore.getEventsConfig(receiverClient, existingStream.getEventsRequested());
        existingStream.setEventsDelivered(eventsConfig.eventsDelivered());

        applySignatureAlgorithmFromClient(existingStream, receiverClient);
        applyUserSubjectFormatFromClient(existingStream, receiverClient);

        existingStream.setUpdatedAt(Time.currentTime());

        streamStore.saveStream(existingStream);

        log.debugf("Stream replaced. realm=%s client=%s streamId=%s",
                session.getContext().getRealm().getName(), session.getContext().getClient().getClientId(), streamUpdate.getStreamId());

        return existingStream;
    }

    /**
     * Reads the receiver client's {@code ssf.signatureAlgorithm} attribute,
     * validates it against {@link SsfSignatureAlgorithms#ALLOWED}, and
     * copies it onto the given {@link StreamConfig} so the dispatcher can
     * pick it up at delivery time. Rejects the stream create/update with
     * {@link SsfException} when the attribute is set to a value the
     * transmitter does not support, giving the receiver a clean 400
     * instead of a silent drop later during SET signing.
     *
     * <p>A {@code null} or blank attribute is intentionally allowed — it
     * means "use the transmitter-wide default", which the dispatcher
     * resolves via {@link SsfSignatureAlgorithms#resolveForStream}.
     */
    protected void applySignatureAlgorithmFromClient(StreamConfig streamConfig, ClientModel receiverClient) {
        String signatureAlgorithm = receiverClient.getAttribute(ClientStreamStore.SSF_STREAM_SIGNATURE_ALGORITHM_KEY);
        if (signatureAlgorithm == null || signatureAlgorithm.isBlank()) {
            streamConfig.setSignatureAlgorithm(null);
            return;
        }
        if (!SsfSignatureAlgorithms.isAllowed(signatureAlgorithm)) {
            throw new SsfException("Invalid stream configuration: signature algorithm " + signatureAlgorithm
                    + " is not in the transmitter allow-list " + SsfSignatureAlgorithms.ALLOWED);
        }
        streamConfig.setSignatureAlgorithm(signatureAlgorithm);
    }

    /**
     * Reads the receiver client's {@code ssf.userSubjectFormat} attribute,
     * validates it against {@link SsfUserSubjectFormats#ALLOWED}, and copies
     * it onto the given {@link StreamConfig} so the mapper can pick it up
     * when building the user portion of an SSF SET. Rejects the stream
     * create/update with {@link SsfException} when the attribute is set to
     * an unsupported format, giving the receiver a clean 400 instead of a
     * silent fallback at emission time.
     *
     * <p>A {@code null} or blank attribute is intentionally allowed — it
     * means "use {@link SsfUserSubjectFormats#DEFAULT}", which matches the
     * transmitter's behavior before this knob was added.
     */
    protected void applyUserSubjectFormatFromClient(StreamConfig streamConfig, ClientModel receiverClient) {
        String userSubjectFormat = receiverClient.getAttribute(ClientStreamStore.SSF_STREAM_USER_SUBJECT_FORMAT_KEY);
        if (userSubjectFormat == null || userSubjectFormat.isBlank()) {
            streamConfig.setUserSubjectFormat(null);
            return;
        }
        if (!SsfUserSubjectFormats.isAllowed(userSubjectFormat)) {
            throw new SsfException("Invalid stream configuration: user subject format " + userSubjectFormat
                    + " is not in the transmitter allow-list " + SsfUserSubjectFormats.ALLOWED);
        }
        streamConfig.setUserSubjectFormat(userSubjectFormat);
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

        log.debugf("Stream deleted. realm=%s client=%s streamId=%s",
                session.getContext().getRealm().getName(), session.getContext().getClient().getClientId(), streamId);

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
        StreamStatus streamStatus = streamStore.updateStreamStatus(streamId, newStreamStatus);

        log.debugf("Stream status updated. realm=%s client=%s streamId=%s status_old=%s status_new=%s",
                session.getContext().getRealm().getName(), session.getContext().getClient().getClientId(), streamId,
                currentStreamStatus.getStatus(), streamStatus.getStatus()
        );

        return streamStatus;
    }
}
