package org.keycloak.ssf.transmitter.stream.storage.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.ssf.Ssf;
import org.keycloak.ssf.SsfProfile;
import org.keycloak.ssf.event.SsfEventRegistry;
import org.keycloak.ssf.stream.StreamStatus;
import org.keycloak.ssf.stream.StreamStatusValue;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.stream.SsfEventsConfig;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamDeliveryConfig;
import org.keycloak.ssf.transmitter.stream.StreamVerificationConfig;
import org.keycloak.ssf.transmitter.stream.VerificationTrigger;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

public class ClientStreamStore implements SsfStreamStore {

    protected static final Logger log = Logger.getLogger(ClientStreamStore.class);

    // ----- Receiver-level configuration (survives stream delete) -------------
    public static final String SSF_ENABLED_KEY = "ssf.enabled";
    public static final String SSF_PROFILE_KEY = "ssf.profile";
    public static final String SSF_VERIFICATION_TRIGGER_KEY = "ssf.verificationTrigger";
    public static final String SSF_VERIFICATION_DELAY_MILLIS_KEY = "ssf.verificationDelayMillis";
    public static final String SSF_LAST_VERIFIED_AT_KEY = "ssf.lastVerifiedAt";
    public static final String SSF_STREAM_AUDIENCE_KEY = "ssf.streamAudience";
    public static final String SSF_STREAM_SUPPORTED_EVENTS_KEY = "ssf.supportedEvents";
    public static final String SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointConnectTimeoutMillis";
    public static final String SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointSocketTimeoutMillis";
    public static final String SSF_STREAM_SIGNATURE_ALGORITHM_KEY = "ssf.signatureAlgorithm";
    public static final String SSF_STREAM_USER_SUBJECT_FORMAT_KEY = "ssf.userSubjectFormat";

    // ----- Per-stream state (cleared on stream delete) -----------------------
    public static final String SSF_STREAM_ID_KEY = "ssf.streamId";
    public static final String SSF_STATUS_KEY = "ssf.status";
    public static final String SSF_STATUS_REASON_KEY = "ssf.status_reason";

    /**
     * Legacy single-blob storage key. Previously the whole {@link StreamConfig}
     * was JSON-serialized into this attribute, which tripped the
     * {@code CLIENT_ATTRIBUTES.VALUE VARCHAR(2048)} column limit on streams
     * with long endpoint URLs or authorization headers. New saves write the
     * split attributes below; reads fall back to this blob if it still exists
     * so pre-refactor data keeps working. {@link #storeStreamConfig} removes
     * it on every save, so the blob is gone for good the first time the stream
     * is updated after the refactor.
     */
    public static final String SSF_STREAM_CONFIG_KEY = "ssf.streamConfig";

    // ----- Per-stream state split across dedicated attributes ----------------
    // Each receiver-writable / transmitter-supplied field gets its own
    // VARCHAR(2048) client attribute. Event sets are stored comma-joined
    // because event-type URIs cannot contain commas.
    public static final String SSF_STREAM_ISSUER_KEY = "ssf.stream.iss";
    public static final String SSF_STREAM_AUDIENCE_STORED_KEY = "ssf.stream.aud";
    public static final String SSF_STREAM_EVENTS_SUPPORTED_KEY = "ssf.stream.eventsSupported";
    public static final String SSF_STREAM_EVENTS_REQUESTED_KEY = "ssf.stream.eventsRequested";
    public static final String SSF_STREAM_EVENTS_DELIVERED_KEY = "ssf.stream.eventsDelivered";
    public static final String SSF_STREAM_DELIVERY_METHOD_KEY = "ssf.stream.delivery.method";
    public static final String SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY = "ssf.stream.delivery.endpointUrl";
    public static final String SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY = "ssf.stream.delivery.authorizationHeader";
    public static final String SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY = "ssf.stream.delivery.additionalParameters";
    public static final String SSF_STREAM_DESCRIPTION_KEY = "ssf.stream.description";
    public static final String SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY = "ssf.stream.minVerificationInterval";
    public static final String SSF_STREAM_INACTIVITY_TIMEOUT_KEY = "ssf.stream.inactivityTimeout";
    public static final String SSF_STREAM_FORMAT_KEY = "ssf.stream.format";
    public static final String SSF_STREAM_CREATED_AT_KEY = "ssf.stream.createdAt";
    public static final String SSF_STREAM_UPDATED_AT_KEY = "ssf.stream.updatedAt";

    private static final String EVENT_SET_DELIMITER = ",";

    /**
     * Attributes that describe a concrete, registered SSF stream for a
     * receiver client. Deleting the stream clears exactly these attributes.
     * Receiver-level configuration survives a stream delete so the receiver
     * can re-register a new stream with the same admin-configured defaults.
     * The legacy {@link #SSF_STREAM_CONFIG_KEY} blob is included here so any
     * pre-refactor leftover is cleaned up on the next delete.
     */
    public static final Set<String> SSF_STREAM_KEYS = Set.of(
            SSF_STREAM_ID_KEY,
            SSF_STATUS_KEY,
            SSF_STATUS_REASON_KEY,
            SSF_STREAM_CONFIG_KEY,
            SSF_STREAM_ISSUER_KEY,
            SSF_STREAM_AUDIENCE_STORED_KEY,
            SSF_STREAM_EVENTS_SUPPORTED_KEY,
            SSF_STREAM_EVENTS_REQUESTED_KEY,
            SSF_STREAM_EVENTS_DELIVERED_KEY,
            SSF_STREAM_DELIVERY_METHOD_KEY,
            SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY,
            SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY,
            SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY,
            SSF_STREAM_DESCRIPTION_KEY,
            SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY,
            SSF_STREAM_INACTIVITY_TIMEOUT_KEY,
            SSF_STREAM_FORMAT_KEY,
            SSF_STREAM_CREATED_AT_KEY,
            SSF_STREAM_UPDATED_AT_KEY);

    protected final KeycloakSession session;

    public ClientStreamStore(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void saveStream(StreamConfig streamConfig) {
        ClientModel client = session.getContext().getClient();
        storeStreamConfig(client, streamConfig);

    }

    @Override
    public StreamStatus updateStreamStatus(String streamId, StreamStatus streamStatus) {
        ClientModel client = session.getContext().getClient();
        StreamConfig streamConfig = extractStreamConfig(client);

        if (streamConfig == null || !streamId.equals(streamConfig.getStreamId())) {
            return null;
        }

        streamConfig.setStatus(StreamStatusValue.valueOf(streamStatus.getStatus()));
        streamConfig.setStatusReason(streamStatus.getReason());
        streamConfig.setUpdatedAt(Time.currentTime());

        // Persist the updated stream config back to the client attribute so
        // subsequent extractStreamConfig calls (e.g. from the dispatcher)
        // observe the new status.
        storeStreamConfig(client, streamConfig);

        String statusReason = streamConfig.getStatusReason();
        StreamStatus status = new StreamStatus();
        status.setStreamId(streamId);
        status.setReason(statusReason);
        status.setStatus(streamConfig.getStatus().getStatusCode());
        return status;
    }

    @Override
    public StreamStatus getStreamStatus(String streamId) {
        ClientModel client = session.getContext().getClient();
        StreamConfig streamConfig = extractStreamConfig(client);

        if (streamConfig == null || !streamId.equals(streamConfig.getStreamId())) {
            return null;
        }

        if (streamConfig.getStatus() == null) {
            return null;
        }

        String statusReason = streamConfig.getStatusReason();
        StreamStatus status = new StreamStatus();
        status.setStreamId(streamId);
        status.setReason(statusReason);
        status.setStatus(streamConfig.getStatus().getStatusCode());
        return status;
    }

    @Override
    public StreamConfig getStream(String streamId) {
        ClientModel client = session.getContext().getClient();
        StreamConfig streamConfig = extractStreamConfig(client);

        if (streamConfig == null || !streamId.equals(streamConfig.getStreamId())) {
            return null;
        }

        if (!Boolean.parseBoolean(client.getAttribute("ssf.enabled"))) {
            return null;
        }

        return streamConfig;
    }

    @Override
    public List<StreamConfig> getAvailableStreams(ClientModel receiverClient) {

        if (receiverClient == null) {
            return List.of();
        }

        StreamConfig streamConfig = extractStreamConfig(receiverClient);

        if (streamConfig == null) {
            return List.of();
        }

        if (!Boolean.parseBoolean(receiverClient.getAttribute("ssf.enabled"))) {
            return List.of();
        }

        return List.of(streamConfig);
    }

    @Override
    public List<StreamConfig> findStreamsForSsfReceiverClients() {
        RealmModel realm = session.getContext().getRealm();
        Map<String, String> attributes = Map.of(SSF_ENABLED_KEY, "true");
        return session.clients()
                .searchClientsByAttributes(realm, attributes, null, null)
                .map(this::extractStreamConfig)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public StreamConfig findStreamById(String streamId) {
        RealmModel realm = session.getContext().getRealm();
        Map<String, String> attributes = Map.of(SSF_STREAM_ID_KEY, streamId);
        return session.clients()
                .searchClientsByAttributes(realm, attributes, 0, 1)
                .map(this::extractStreamConfig)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void deleteStream(String streamId) {
        ClientModel client = session.getContext().getClient();
        deleteStreamConfig(client, streamId);
    }

    @Override
    public void recordStreamVerification(String streamId) {
        if (streamId == null) {
            return;
        }
        findClientByStreamId(streamId, session.getContext().getRealm()).ifPresent(this::updateLastVerified);
    }

    protected void updateLastVerified(ClientModel client) {
        client.setAttribute(SSF_LAST_VERIFIED_AT_KEY,String.valueOf(Time.currentTime()));
    }

    protected Optional<ClientModel> findClientByStreamId(String streamId, RealmModel realm) {
        return session.clients()
                .searchClientsByAttributes(realm, Map.of(SSF_STREAM_ID_KEY, streamId), 0, 1)
                .findFirst();
    }

    public StreamConfig getStreamForClient(ClientModel client) {
        return extractStreamConfig(client);
    }

    /**
     * Removes the SSF stream registration (and all associated attributes) from
     * the given client. Returns {@code true} if a stream existed and was
     * removed, {@code false} if no stream was registered for the client.
     */
    public boolean deleteStreamForClient(ClientModel client) {
        StreamConfig streamConfig = extractStreamConfig(client);
        if (streamConfig == null) {
            return false;
        }
        SSF_STREAM_KEYS.forEach(client::removeAttribute);
        return true;
    }

    protected StreamConfig extractStreamConfig(ClientModel client) {
        if (client == null) {
            return null;
        }

        String streamId = client.getAttribute(SSF_STREAM_ID_KEY);
        if (streamId == null) {
            return null;
        }

        // Prefer the legacy single-blob attribute if it is still present so
        // upgrades from pre-refactor installs keep working. The next
        // storeStreamConfig() call will write the split attributes and
        // remove the blob, at which point subsequent reads take the split
        // path below.
        String legacyBlob = client.getAttribute(SSF_STREAM_CONFIG_KEY);
        StreamConfig streamConfig = legacyBlob != null
                ? extractFromLegacyBlob(client, streamId, legacyBlob)
                : extractFromSplitAttributes(client, streamId);
        if (streamConfig == null) {
            return null;
        }

        // Stamp the receiver client's Keycloak id on every read so the
        // dispatcher / outbox can identify the owning client without a
        // second lookup. Not persisted, not echoed on the wire — covered
        // by @JsonIgnore on the field.
        streamConfig.setClientId(client.getId());

        applyReceiverAttributeOverlays(client, streamConfig);
        return streamConfig;
    }

    private StreamConfig extractFromLegacyBlob(ClientModel client, String streamId, String legacyBlob) {
        try {
            return JsonSerialization.readValue(legacyBlob, StreamConfig.class);
        } catch (IOException e) {
            log.errorf(e,
                    "Failed to deserialize legacy stream configuration blob. clientId=%s streamId=%s",
                    client.getClientId(), streamId);
            return null;
        }
    }

    private StreamConfig extractFromSplitAttributes(ClientModel client, String streamId) {
        StreamConfig streamConfig = new StreamConfig();
        streamConfig.setStreamId(streamId);
        streamConfig.setIssuer(client.getAttribute(SSF_STREAM_ISSUER_KEY));
        streamConfig.setAudience(splitSet(client.getAttribute(SSF_STREAM_AUDIENCE_STORED_KEY)));
        streamConfig.setEventsSupported(splitSet(client.getAttribute(SSF_STREAM_EVENTS_SUPPORTED_KEY)));
        streamConfig.setEventsRequested(splitSet(client.getAttribute(SSF_STREAM_EVENTS_REQUESTED_KEY)));
        streamConfig.setEventsDelivered(splitSet(client.getAttribute(SSF_STREAM_EVENTS_DELIVERED_KEY)));
        streamConfig.setDescription(client.getAttribute(SSF_STREAM_DESCRIPTION_KEY));
        streamConfig.setMinVerificationInterval(parseIntAttribute(client, SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY));
        streamConfig.setInactivityTimeout(parseIntAttribute(client, SSF_STREAM_INACTIVITY_TIMEOUT_KEY));
        streamConfig.setFormat(client.getAttribute(SSF_STREAM_FORMAT_KEY));
        streamConfig.setCreatedAt(parseIntAttribute(client, SSF_STREAM_CREATED_AT_KEY));
        streamConfig.setUpdatedAt(parseIntAttribute(client, SSF_STREAM_UPDATED_AT_KEY));

        String deliveryMethod = client.getAttribute(SSF_STREAM_DELIVERY_METHOD_KEY);
        if (deliveryMethod != null) {
            StreamDeliveryConfig delivery = new StreamDeliveryConfig();
            delivery.setMethod(deliveryMethod);
            delivery.setEndpointUrl(client.getAttribute(SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY));
            delivery.setAuthorizationHeader(client.getAttribute(SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY));
            String additionalRaw = client.getAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY);
            if (additionalRaw != null) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> additionalParameters =
                            JsonSerialization.readValue(additionalRaw, Map.class);
                    delivery.setAdditionalParameters(additionalParameters);
                } catch (IOException e) {
                    log.warnf(e,
                            "Failed to deserialize delivery.additional_parameters; ignoring. clientId=%s streamId=%s",
                            client.getClientId(), streamId);
                }
            }
            streamConfig.setDelivery(delivery);
        }

        String statusValue = client.getAttribute(SSF_STATUS_KEY);
        if (statusValue != null) {
            try {
                streamConfig.setStatus(StreamStatusValue.valueOf(statusValue));
            } catch (IllegalArgumentException e) {
                log.warnf("Unknown stream status '%s' on client %s; leaving unset",
                        statusValue, client.getClientId());
            }
        }
        streamConfig.setStatusReason(client.getAttribute(SSF_STATUS_REASON_KEY));

        return streamConfig;
    }

    private void applyReceiverAttributeOverlays(ClientModel client, StreamConfig streamConfig) {
        // Receiver-level defaults that the admin configures as client
        // attributes. These overlay whatever came out of the per-stream
        // state so the dispatcher sees the effective values at emission time.
        if (client.getAttribute(SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY) != null) {
            streamConfig.setPushEndpointConnectTimeoutMillis(
                    Integer.parseInt(client.getAttribute(SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY)));
        }
        if (client.getAttribute(SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY) != null) {
            streamConfig.setPushEndpointSocketTimeoutMillis(
                    Integer.parseInt(client.getAttribute(SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY)));
        }
        if (client.getAttribute(SSF_PROFILE_KEY) != null) {
            streamConfig.setProfile(SsfProfile.valueOf(client.getAttribute(SSF_PROFILE_KEY)));
        }
        String signatureAlgorithm = client.getAttribute(SSF_STREAM_SIGNATURE_ALGORITHM_KEY);
        if (signatureAlgorithm != null && !signatureAlgorithm.isBlank()) {
            streamConfig.setSignatureAlgorithm(signatureAlgorithm);
        }
        String userSubjectFormat = client.getAttribute(SSF_STREAM_USER_SUBJECT_FORMAT_KEY);
        if (userSubjectFormat != null && !userSubjectFormat.isBlank()) {
            streamConfig.setUserSubjectFormat(userSubjectFormat);
        }
    }

    protected void storeStreamConfig(ClientModel client, StreamConfig streamConfig) {

        // Remove any pre-refactor single-blob leftover so extractStreamConfig
        // takes the split-attribute path on subsequent reads instead of the
        // legacy fallback. This is what actually migrates a stream off the
        // old storage shape — it happens on the first update after upgrade.
        client.removeAttribute(SSF_STREAM_CONFIG_KEY);

        client.setAttribute(SSF_STREAM_ID_KEY, streamConfig.getStreamId());

        StreamStatusValue status = streamConfig.getStatus();
        if (status == null) {
            status = StreamStatusValue.enabled;
        }
        client.setAttribute(SSF_STATUS_KEY, status.name());
        setOrRemove(client, SSF_STATUS_REASON_KEY, streamConfig.getStatusReason());

        setOrRemove(client, SSF_STREAM_ISSUER_KEY, streamConfig.getIssuer());
        setOrRemove(client, SSF_STREAM_AUDIENCE_STORED_KEY, joinSet(streamConfig.getAudience()));
        setOrRemove(client, SSF_STREAM_EVENTS_SUPPORTED_KEY, joinSet(streamConfig.getEventsSupported()));
        setOrRemove(client, SSF_STREAM_EVENTS_REQUESTED_KEY, joinSet(streamConfig.getEventsRequested()));
        setOrRemove(client, SSF_STREAM_EVENTS_DELIVERED_KEY, joinSet(streamConfig.getEventsDelivered()));
        setOrRemove(client, SSF_STREAM_DESCRIPTION_KEY, streamConfig.getDescription());
        setIntOrRemove(client, SSF_STREAM_MIN_VERIFICATION_INTERVAL_KEY, streamConfig.getMinVerificationInterval());
        setIntOrRemove(client, SSF_STREAM_INACTIVITY_TIMEOUT_KEY, streamConfig.getInactivityTimeout());
        setOrRemove(client, SSF_STREAM_FORMAT_KEY, streamConfig.getFormat());
        setIntOrRemove(client, SSF_STREAM_CREATED_AT_KEY, streamConfig.getCreatedAt());
        setIntOrRemove(client, SSF_STREAM_UPDATED_AT_KEY, streamConfig.getUpdatedAt());

        StreamDeliveryConfig delivery = streamConfig.getDelivery();
        if (delivery != null) {
            setOrRemove(client, SSF_STREAM_DELIVERY_METHOD_KEY, delivery.getMethod());
            setOrRemove(client, SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY, delivery.getEndpointUrl());
            setOrRemove(client, SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY, delivery.getAuthorizationHeader());
            Map<String, Object> additionalParameters = delivery.getAdditionalParameters();
            if (additionalParameters != null && !additionalParameters.isEmpty()) {
                client.setAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY,
                        JsonSerialization.valueAsString(additionalParameters));
            } else {
                client.removeAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY);
            }
        } else {
            client.removeAttribute(SSF_STREAM_DELIVERY_METHOD_KEY);
            client.removeAttribute(SSF_STREAM_DELIVERY_ENDPOINT_URL_KEY);
            client.removeAttribute(SSF_STREAM_DELIVERY_AUTHORIZATION_HEADER_KEY);
            client.removeAttribute(SSF_STREAM_DELIVERY_ADDITIONAL_PARAMETERS_KEY);
        }
    }

    private static void setOrRemove(ClientModel client, String key, String value) {
        if (value == null) {
            client.removeAttribute(key);
        } else {
            client.setAttribute(key, value);
        }
    }

    private static void setIntOrRemove(ClientModel client, String key, Integer value) {
        if (value == null) {
            client.removeAttribute(key);
        } else {
            client.setAttribute(key, Integer.toString(value));
        }
    }

    private static Integer parseIntAttribute(ClientModel client, String key) {
        String raw = client.getAttribute(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException e) {
            log.warnf("Invalid numeric client attribute %s='%s' on %s; ignoring",
                    key, raw, client.getClientId());
            return null;
        }
    }

    private static String joinSet(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return String.join(EVENT_SET_DELIMITER, set);
    }

    private static Set<String> splitSet(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // Preserve insertion order so a round-trip through storage keeps
        // the same JSON shape.
        Set<String> result = new LinkedHashSet<>();
        for (String entry : value.split(EVENT_SET_DELIMITER)) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? null : result;
    }

    protected void deleteStreamConfig(ClientModel client, String streamId) {
        StreamConfig streamConfig = extractStreamConfig(client);
        if (streamConfig != null && streamConfig.getStreamId().equals(streamId)) {
            SSF_STREAM_KEYS.forEach(client::removeAttribute);
        }
    }

    @Override
    public StreamVerificationConfig getStreamVerificationConfig(String streamId, ClientModel client) {

        VerificationTrigger verificationTrigger = getVerificationTrigger(client);
        int verificationDelayMillis = getVerificationDelayMillis(client);

        return new StreamVerificationConfig(verificationTrigger, verificationDelayMillis);
    }

    protected int getVerificationDelayMillis(ClientModel client) {
        if (client.getAttribute(SSF_VERIFICATION_DELAY_MILLIS_KEY) != null) {
            return Integer.parseInt(client.getAttribute(SSF_VERIFICATION_DELAY_MILLIS_KEY));
        }
        // Fallback to the transmitter-wide default configured via SPI
        return session.getProvider(SsfTransmitterProvider.class).getConfig().getTransmitterInitiatedVerificationDelayMillis();
    }

    protected VerificationTrigger getVerificationTrigger(ClientModel client) {
        return client.getAttribute(SSF_VERIFICATION_TRIGGER_KEY) != null ? VerificationTrigger.valueOf(client.getAttribute(SSF_VERIFICATION_TRIGGER_KEY)) : null;
    }

    @Override
    public SsfEventsConfig getEventsConfig(ClientModel client, Set<String> eventsRequested) {

        Set<String> supportedEvents = getSupportedEvents(session, client);

        // TODO compute events delivered for current realm
        Set<String> eventsDelivered = new HashSet<>(eventsRequested);
        eventsDelivered.retainAll(supportedEvents);

        return new SsfEventsConfig(supportedEvents, eventsRequested, eventsDelivered);
    }

    protected Set<String> getSupportedEvents(KeycloakSession session, ClientModel client) {

        SsfTransmitterProvider transmitter = session.getProvider(SsfTransmitterProvider.class);
        SsfEventRegistry registry = Ssf.events().getRegistry();

        String supportedEventsAttribute = client.getAttribute(SSF_STREAM_SUPPORTED_EVENTS_KEY);
        if (supportedEventsAttribute == null) {

            return transmitter.getDefaultSupportedEvents();
        }

        // The admin UI stores event aliases (e.g. "CaepCredentialChange") in the
        // client attribute; the SSF stream configuration however MUST carry the
        // full event type URIs as defined by the SSF/CAEP/RISC specs. Resolve
        // each candidate (alias or URI) to its canonical event type URI and
        // drop any candidates the transmitter does not know about.
        Set<String> supportedEvents = new TreeSet<>();
        Set<String> supportedEventCandidates = SsfUtil.parseEventTypeAliases(supportedEventsAttribute);
        for (String supportedEventCandidate : supportedEventCandidates) {

            String eventType = registry.resolveEventTypeForAlias(supportedEventCandidate);
            if (eventType == null && registry.getEventClassByType(supportedEventCandidate).isPresent()) {
                eventType = supportedEventCandidate;
            }
            if (eventType == null) {
                continue;
            }
            supportedEvents.add(eventType);
        }
        return supportedEvents;
    }
}
