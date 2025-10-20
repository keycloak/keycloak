package org.keycloak.protocol.ssf.receiver;

import org.keycloak.protocol.ssf.event.delivery.DeliveryMethod;

import java.util.Set;

public class ReceiverConfig {

    protected String alias;

    protected String description;

    protected String transmitterUrl;

    protected String transmitterConfigUrl;

    protected String transmitterPollUrl;

    protected String transmitterAccessToken;

    protected Boolean managedStream;

    protected DeliveryMethod deliveryMethod;

    protected String pushAuthorizationToken;

    protected String receiverPushUrl;

    protected int pollIntervalSeconds;

    protected Set<String> eventsRequested;

    protected String providerId;

    protected String streamId;

    protected Integer maxEvents;

    protected Boolean acknowledgeImmediately;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransmitterUrl() {
        return transmitterUrl;
    }

    public void setTransmitterUrl(String transmitterUrl) {
        this.transmitterUrl = transmitterUrl;
    }

    public String getTransmitterConfigUrl() {
        return transmitterConfigUrl;
    }

    public void setTransmitterConfigUrl(String transmitterConfigUrl) {
        this.transmitterConfigUrl = transmitterConfigUrl;
    }

    public String getTransmitterPollUrl() {
        return transmitterPollUrl;
    }

    public void setTransmitterPollUrl(String transmitterPollUrl) {
        this.transmitterPollUrl = transmitterPollUrl;
    }

    public String getTransmitterAccessToken() {
        return transmitterAccessToken;
    }

    public void setTransmitterAccessToken(String transmitterAccessToken) {
        this.transmitterAccessToken = transmitterAccessToken;
    }

    public Boolean getManagedStream() {
        return managedStream;
    }

    public void setManagedStream(Boolean managedStream) {
        this.managedStream = managedStream;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getPushAuthorizationToken() {
        return pushAuthorizationToken;
    }

    public void setPushAuthorizationToken(String pushAuthorizationToken) {
        this.pushAuthorizationToken = pushAuthorizationToken;
    }

    public String getReceiverPushUrl() {
        return receiverPushUrl;
    }

    public void setReceiverPushUrl(String receiverPushUrl) {
        this.receiverPushUrl = receiverPushUrl;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public Set<String> getEventsRequested() {
        return eventsRequested;
    }

    public void setEventsRequested(Set<String> eventsRequested) {
        this.eventsRequested = eventsRequested;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public Integer getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(Integer maxEvents) {
        this.maxEvents = maxEvents;
    }

    public Boolean getAcknowledgeImmediately() {
        return acknowledgeImmediately;
    }

    public void setAcknowledgeImmediately(Boolean acknowledgeImmediately) {
        this.acknowledgeImmediately = acknowledgeImmediately;
    }
}
