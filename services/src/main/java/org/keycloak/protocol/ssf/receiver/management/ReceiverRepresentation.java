package org.keycloak.protocol.ssf.receiver.management;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReceiverRepresentation {

    protected String alias;

    protected String componentId;

    protected String description;

    protected String streamId;

    protected Set<String> audience;

    protected Set<String> eventsDelivered;

    protected Boolean managedStream;

    protected String deliveryMethod;

    protected String transmitterUrl;

    protected String transmitterPollUrl;

    protected Integer pollIntervalSeconds;

    protected String receiverPushUrl;

    protected String pushAuthorizationToken;

    protected int configHash;

    protected long modifiedAt;

    protected int maxEvents;

    protected boolean acknowledgeImmediately;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(Set<String> audience) {
        this.audience = audience;
    }

    public Set<String> getEventsDelivered() {
        return eventsDelivered;
    }

    public void setEventsDelivered(Set<String> eventsDelivered) {
        this.eventsDelivered = eventsDelivered;
    }

    public Boolean getManagedStream() {
        return managedStream;
    }

    public void setManagedStream(Boolean managedStream) {
        this.managedStream = managedStream;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getTransmitterUrl() {
        return transmitterUrl;
    }

    public void setTransmitterUrl(String transmitterUrl) {
        this.transmitterUrl = transmitterUrl;
    }

    public String getTransmitterPollUrl() {
        return transmitterPollUrl;
    }

    public void setTransmitterPollUrl(String transmitterPollUrl) {
        this.transmitterPollUrl = transmitterPollUrl;
    }

    public Integer getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(Integer pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public String getReceiverPushUrl() {
        return receiverPushUrl;
    }

    public void setReceiverPushUrl(String receiverPushUrl) {
        this.receiverPushUrl = receiverPushUrl;
    }

    public String getPushAuthorizationToken() {
        return pushAuthorizationToken;
    }

    public void setPushAuthorizationToken(String pushAuthorizationToken) {
        this.pushAuthorizationToken = pushAuthorizationToken;
    }

    public int getConfigHash() {
        return configHash;
    }

    public void setConfigHash(int configHash) {
        this.configHash = configHash;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public int getMaxEvents() {
        return maxEvents;
    }

    public void setMaxEvents(int maxEvents) {
        this.maxEvents = maxEvents;
    }

    public boolean isAcknowledgeImmediately() {
        return acknowledgeImmediately;
    }

    public void setAcknowledgeImmediately(boolean acknowledgeImmediately) {
        this.acknowledgeImmediately = acknowledgeImmediately;
    }
}
