package org.keycloak.ssf.transmitter.stream.storage.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.keycloak.ssf.transmitter.SsfTransmitter;
import org.keycloak.ssf.transmitter.SsfTransmitterProvider;
import org.keycloak.ssf.transmitter.support.SsfUtil;
import org.keycloak.ssf.transmitter.stream.SsfEventsConfig;
import org.keycloak.ssf.transmitter.stream.VerificationTrigger;
import org.keycloak.ssf.transmitter.stream.StreamConfig;
import org.keycloak.ssf.transmitter.stream.StreamVerificationConfig;
import org.keycloak.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

public class ClientStreamStore implements SsfStreamStore {

    protected static final Logger log = Logger.getLogger(ClientStreamStore.class);

    public static final String SSF_ENABLED_KEY = "ssf.enabled";
    public static final String SSF_PROFILE_KEY = "ssf.profile";
    public static final String SSF_STREAM_ID_KEY = "ssf.streamId";
    public static final String SSF_VERIFICATION_TRIGGER_KEY = "ssf.verificationTrigger";
    public static final String SSF_VERIFICATION_DELAY_MILLIS_KEY = "ssf.verificationDelayMillis";
    public static final String SSF_STREAM_CONFIG_KEY = "ssf.streamConfig";
    public static final String SSF_STREAM_AUDIENCE_KEY = "ssf.streamAudience";
    public static final String SSF_STREAM_SUPPORTED_EVENTS_KEY = "ssf.supportedEvents";
    public static final String SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointConnectTimeoutMillis";
    public static final String SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointSocketTimeoutMillis";
    public static final String SSF_STREAM_SIGNATURE_ALGORITHM_KEY = "ssf.signatureAlgorithm";
    public static final String SSF_STREAM_USER_SUBJECT_FORMAT_KEY = "ssf.userSubjectFormat";

    public static final String SSF_STATUS_KEY = "ssf.status";
    public static final String SSF_STATUS_REASON_KEY = "ssf.status_reason";

    /**
     * Attributes that describe a concrete, registered SSF stream for a
     * receiver client. Deleting the stream clears exactly these attributes.
     * receiver-level configuration survives a stream delete so the receiver can re-register
     * a new stream with the same admin-configured defaults.
     */
    public static final Set<String> SSF_STREAM_KEYS = Set.of(
            SSF_STREAM_ID_KEY,
            SSF_STREAM_CONFIG_KEY,
            SSF_STATUS_KEY,
            SSF_STATUS_REASON_KEY);

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
    public List<StreamConfig> getAvailableStreams() {
        ClientModel client = session.getContext().getClient();
        StreamConfig streamConfig = extractStreamConfig(client);

        if (streamConfig == null) {
            return List.of();
        }

        if (!Boolean.parseBoolean(client.getAttribute("ssf.enabled"))) {
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

        String streamConfigRaw = client.getAttribute(SSF_STREAM_CONFIG_KEY);
        if (streamConfigRaw == null) {
            return null;
        }

        try {
            StreamConfig streamConfig = JsonSerialization.readValue(streamConfigRaw, StreamConfig.class);

            if (client.getAttribute(SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY) != null) {
                streamConfig.setPushEndpointConnectTimeoutMillis(Integer.parseInt(client.getAttribute(SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY)));
            }

            if (client.getAttribute(SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY) != null) {
                streamConfig.setPushEndpointSocketTimeoutMillis(Integer.parseInt(client.getAttribute(SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY)));
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

            return streamConfig;
        } catch (IOException e) {
            log.errorf(e, "Failed to deserialize stream configuration from client. clientId=%s streamId=%s"
                    , client.getClientId(), streamId);
            return null;
        }
    }

    protected void storeStreamConfig(ClientModel client, StreamConfig streamConfig) {

        client.setAttribute(SSF_STREAM_ID_KEY, streamConfig.getStreamId());

        StreamStatusValue status = streamConfig.getStatus();
        if (status == null) {
            status = StreamStatusValue.enabled;
        }
        client.setAttribute(SSF_STATUS_KEY, status.name());
        client.setAttribute(SSF_STATUS_REASON_KEY, streamConfig.getStatusReason());
        client.setAttribute(SSF_STREAM_CONFIG_KEY, JsonSerialization.valueAsString(streamConfig));
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
        return SsfTransmitter.current().getConfig().getTransmitterInitiatedVerificationDelayMillis();
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

        SsfTransmitterProvider transmitter = SsfTransmitter.current();
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
