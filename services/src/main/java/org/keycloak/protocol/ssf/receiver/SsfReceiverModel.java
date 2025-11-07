package org.keycloak.protocol.ssf.receiver;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.protocol.ssf.event.DeliveryMethod;
import org.keycloak.protocol.ssf.stream.StreamStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class SsfReceiverModel extends ComponentModel {

    public static final int DEFAULT_MAX_EVENTS = 32;

    private SsfReceiverProviderConfig receiverProviderConfig;

    public SsfReceiverModel() {
    }

    public SsfReceiverModel(ComponentModel model) {
        this(model, null);
    }

    public SsfReceiverModel(ComponentModel model, SsfReceiverProviderConfig receiverProviderConfig) {
        super(model);
        this.receiverProviderConfig = receiverProviderConfig;
    }

    public static SsfReceiverModel create(String alias, SsfReceiverConfig config) {

        SsfReceiverModel model = new SsfReceiverModel();
        model.setAlias(alias);
        model.setDescription(config.getDescription());

        model.setTransmitterAccessToken(config.getTransmitterAccessToken());
        if (config.getPushAuthorizationHeader() != null) {
            model.setPushAuthorizationHeader(config.getPushAuthorizationHeader());
        }

        String transmitterUrl = Objects.requireNonNull(config.getTransmitterUrl(), "transmitterUrl");
        model.setTransmitterUrl(transmitterUrl);

        String transmitterConfigUrl = config.getTransmitterConfigUrl();
        if (transmitterConfigUrl == null) {
            String configUrl = transmitterUrl;
            if (!configUrl.endsWith("/")) {
                configUrl+="/";
            }
            configUrl = configUrl + ".well-known/ssf-configuration";
            transmitterConfigUrl = configUrl;
        }
        model.setTransmitterConfigUrl(transmitterConfigUrl);

        model.setTransmitterPollUrl(config.getTransmitterPollUrl());
        model.setPollIntervalSeconds(config.getPollIntervalSeconds());
        model.setManagedStream(config.getManagedStream());

        if (config.getMaxEvents() != null) {
            model.setMaxEvents(config.getMaxEvents());
        } else {
            model.setMaxEvents(DEFAULT_MAX_EVENTS);
        }

        if (Boolean.TRUE.equals(config.getAcknowledgeImmediately())) {
            model.setAcknowledgeImmediately(config.getAcknowledgeImmediately());
        } else {
            model.setAcknowledgeImmediately(false);
        }

        if (Boolean.TRUE.equals(model.getManagedStream())) {
            model.setEventsRequested(config.getEventsRequested());
            model.setDeliveryMethod(config.getDeliveryMethod());
        } else {
            String streamId = Objects.requireNonNull(config.getStreamId(), "streamId");
            model.setStreamId(streamId);
        }

        return model;
    }

    public SsfReceiverProviderConfig getReceiverProviderConfig() {
        return receiverProviderConfig;
    }

    public void setReceiverProviderConfig(SsfReceiverProviderConfig receiverProviderConfig) {
        this.receiverProviderConfig = receiverProviderConfig;
    }

    public void setIssuer(String issuer) {
        getConfig().putSingle("issuer", issuer);
    }

    public String getIssuer() {
        return getConfig().getFirst("issuer");
    }

    public void setJwksUri(String issuer) {
        getConfig().putSingle("jwksUri", issuer);
    }

    public String getJwksUri() {
        return getConfig().getFirst("jwksUri");
    }

    public String getStreamId() {
        return getConfig().getFirst("streamId");
    }

    public void setStreamId(String streamId) {
        getConfig().putSingle("streamId", streamId);
    }

    public StreamStatus getStreamStatus() {
        return StreamStatus.valueOf(getConfig().getFirst("streamStatus"));
    }

    public void setStreamStatus(StreamStatus status) {
        getConfig().putSingle("streamStatus", status.name());
    }

    public String getTransmitterUrl() {
        return getConfig().getFirst("transmitterUrl");
    }

    public void setTransmitterUrl(String transmitterUrl) {
        getConfig().putSingle("transmitterUrl", transmitterUrl);
    }

    public String getTransmitterConfigUrl() {
        return getConfig().getFirst("transmitterConfigUrl");
    }

    public void setTransmitterConfigUrl(String transmitterConfigUrl) {
        getConfig().putSingle("transmitterConfigUrl", transmitterConfigUrl);
    }

    public String getTransmitterPollUrl() {
        return getConfig().getFirst("transmitterPollUrl");
    }

    public void setTransmitterPollUrl(String transmitterPollUrl) {
        getConfig().putSingle("transmitterPollUrl", transmitterPollUrl);
    }

    public String getReceiverPushUrl() {
        return getConfig().getFirst("receiverPushUrl");
    }

    public void setReceiverPushUrl(String receiverPushUrl) {
        getConfig().putSingle("receiverPushUrl", receiverPushUrl);
    }

    public DeliveryMethod getDeliveryMethod() {
        return DeliveryMethod.valueOf(getConfig().getFirst("deliveryMethod"));
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        getConfig().putSingle("deliveryMethod", deliveryMethod.name());
    }

    public Boolean getManagedStream() {
        return Boolean.valueOf(getConfig().getFirst("managedStream"));
    }

    public void setManagedStream(Boolean managedStream) {
        getConfig().putSingle("managedStream", Boolean.toString(Boolean.TRUE.equals(managedStream)));
    }

    public Integer getPollIntervalSeconds() {
        String pollIntervalSeconds = getConfig().getFirst("pollIntervalSeconds");
        if (pollIntervalSeconds == null || pollIntervalSeconds.isEmpty()) {
            return null;
        }

        return Integer.parseInt(pollIntervalSeconds);
    }

    public void setPollIntervalSeconds(Integer pollIntervalSeconds) {
        if (pollIntervalSeconds != null) {
            getConfig().putSingle("pollIntervalSeconds", Integer.toString(pollIntervalSeconds));
        }
    }

    public String getTransmitterAccessToken() {
        return getConfig().getFirst("transmitterAccessToken");
    }

    public void setTransmitterAccessToken(String transmitterAccessToken) {
        getConfig().putSingle("transmitterAccessToken", transmitterAccessToken);
    }

    public String getDescription() {
        return getConfig().getFirst("description");
    }

    public void setDescription(String description) {
        getConfig().putSingle("description", description);
    }

    public Set<String> getEventsRequested() {
        List<String> eventsRequested = getConfig().getList("eventsRequested");
        if (eventsRequested == null || eventsRequested.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(new TreeSet<>(eventsRequested));
    }

    public void setEventsRequested(Set<String> eventsRequested) {
        getConfig().put("eventsRequested", eventsRequested.stream().toList());
    }

    public Set<String> getEventsDelivered() {
        List<String> eventsDelivered = getConfig().getList("eventsDelivered");
        if (eventsDelivered == null || eventsDelivered.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(new TreeSet<>(eventsDelivered));
    }

    public void setEventsDelivered(Set<String> eventsDelivered) {
        getConfig().put("eventsDelivered", eventsDelivered.stream().toList());
    }

    public String getAlias() {
        return getConfig().getFirst("alias");
    }

    public void setAlias(String alias) {
        getConfig().putSingle("alias", alias);
    }

    public boolean isPollDelivery() {
        return DeliveryMethod.POLL.equals(getDeliveryMethod());
    }

    public void setAudience(Set<String> audience) {
        getConfig().put("audience", new ArrayList<>(audience));
    }

    public Set<String> getAudience() {
        List<String> audience = getConfig().getList("audience");
        if (audience == null || audience.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(audience);
    }

    public void setModifiedAt(long timestamp) {
        getConfig().putSingle("modifiedAt", Long.toString(timestamp));
    }

    public long getModifiedAt() {
        String modifiedAt = getConfig().getFirst("modifiedAt");
        if (modifiedAt == null || modifiedAt.isEmpty()) {
            return -1L;
        }
        return Long.parseLong(modifiedAt);
    }

    public void setMaxEvents(int maxEvents) {
        getConfig().putSingle("maxEvents", Integer.toString(maxEvents));
    }

    public int getMaxEvents() {
        String maxEvents = getConfig().getFirst("maxEvents");
        if (maxEvents == null || maxEvents.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(maxEvents);
    }

    public boolean isAcknowledgeImmediately() {
        return Boolean.parseBoolean(getConfig().getFirst("acknowledgeImmediately"));
    }

    public void setAcknowledgeImmediately(boolean acknowledgeImmediately) {
        getConfig().putSingle("acknowledgeImmediately", Boolean.toString(acknowledgeImmediately));
    }


    public static int computeConfigHash(SsfReceiverModel receiverModel) {
        var copy = new MultivaluedHashMap<>(receiverModel.getConfig());
        copy.remove("modifiedAt");
        copy.remove("configHash");
        return copy.hashCode();
    }

    public int getConfigHash() {
        String configHash = getConfig().getFirst("configHash");
        if (configHash == null || configHash.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(configHash);
    }

    public void setConfigHash(int configHash) {
        getConfig().putSingle("configHash", Integer.toString(configHash));
    }

    public void setPushAuthorizationHeader(String authorizationHeader) {
        getConfig().putSingle("pushAuthorizationHeader", authorizationHeader);
    }

    public String getPushAuthorizationHeader() {
        return getConfig().getFirst("pushAuthorizationHeader");
    }

    public int getConnectTimeout() {
        String timeout = getConfig().getFirst("connectTimeout");
        if (timeout == null || timeout.isEmpty()) {
            return 3000;
        }
        return Integer.parseInt(timeout);
    }

    public void setConnectTimeout(int timeout) {
        getConfig().putSingle("connectTimeout", Integer.toString(timeout));
    }

    public int getSocketTimeout() {
        String timeout = getConfig().getFirst("socketTimeout");
        if (timeout == null || timeout.isEmpty()) {
            return 3000;
        }
        return Integer.parseInt(timeout);
    }

    public void setSocketTimeout(int timeout) {
        getConfig().putSingle("socketTimeout", Integer.toString(timeout));
    }
}
