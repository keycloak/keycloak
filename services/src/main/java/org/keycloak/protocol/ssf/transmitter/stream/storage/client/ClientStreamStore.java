package org.keycloak.protocol.ssf.transmitter.stream.storage.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ssf.Ssf;
import org.keycloak.protocol.ssf.SsfProfile;
import org.keycloak.protocol.ssf.stream.StreamStatus;
import org.keycloak.protocol.ssf.stream.StreamStatusValue;
import org.keycloak.protocol.ssf.transmitter.stream.SsfVerificationTrigger;
import org.keycloak.protocol.ssf.transmitter.stream.StreamConfig;
import org.keycloak.protocol.ssf.transmitter.stream.storage.SsfStreamStore;
import org.keycloak.protocol.ssf.transmitter.stream.StreamVerificationConfig;
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
    public static final String SSF_PUSH_ENDPOINT_CONNECT_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointConnectTimeoutMillis";
    public static final String SSF_PUSH_ENDPOINT_SOCKET_TIMEOUT_MILLIS_KEY = "ssf.pushEndpointSocketTimeoutMillis";

    public static final String SSF_STATUS_KEY = "ssf.status";
    public static final String SSF_STATUS_REASON_KEY = "ssf.status_reason";

    public static final Set<String> SSF_STREAM_KEYS = Set.of(
            SSF_ENABLED_KEY, SSF_PROFILE_KEY, SSF_STREAM_ID_KEY, SSF_VERIFICATION_TRIGGER_KEY,
            SSF_VERIFICATION_DELAY_MILLIS_KEY, SSF_STREAM_CONFIG_KEY, SSF_STATUS_KEY, SSF_STATUS_REASON_KEY);

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
    public List<StreamConfig> findAllEnabledStreams() {
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

        SsfVerificationTrigger verificationTrigger = getVerificationTrigger(client);
        int verificationDelayMillis = getVerificationDelayMillis(client);

        return new StreamVerificationConfig(verificationTrigger, verificationDelayMillis);
    }

    protected int getVerificationDelayMillis(ClientModel client) {
        if (client.getAttribute(SSF_VERIFICATION_DELAY_MILLIS_KEY) != null) {
            return Integer.parseInt(client.getAttribute(SSF_VERIFICATION_DELAY_MILLIS_KEY));
        }
        // Fallback to default value
        return Ssf.TRANSMITTER_INITIATED_VERIFICATION_DELAY_MILLIS;
    }

    protected SsfVerificationTrigger getVerificationTrigger(ClientModel client) {
        return client.getAttribute(SSF_VERIFICATION_TRIGGER_KEY) != null ? SsfVerificationTrigger.valueOf(client.getAttribute(SSF_VERIFICATION_TRIGGER_KEY)) : null;
    }
}
