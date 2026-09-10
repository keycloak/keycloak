package org.keycloak.ssf.transmitter.admin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Admin-facing snapshot of a single SSF outbox row, returned by the
 * admin lookup-by-jti endpoint. Covers any outbox status — PENDING,
 * HELD, DELIVERED, DEAD_LETTER — so the operator can answer "where is
 * this event in the delivery pipeline?" regardless of whether it's
 * still queued, was delivered, or terminally failed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SsfEventRepresentation {

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("deliveryMethod")
    private String deliveryMethod;

    @JsonProperty("status")
    private String status;

    @JsonProperty("attempts")
    private int attempts;

    @JsonProperty("createdAt")
    private Long createdAt;

    @JsonProperty("nextAttemptAt")
    private Long nextAttemptAt;

    @JsonProperty("deliveredAt")
    private Long deliveredAt;

    @JsonProperty("lastError")
    private String lastError;

    @JsonProperty("streamId")
    private String streamId;

    /**
     * Decoded Security Event Token (JWS payload) — the full claim set
     * the receiver processes, verbatim. Includes the transmitter-
     * supplied header claims ({@code iss}, {@code iat}, {@code jti},
     * {@code aud}, {@code txn}), the subject ({@code sub_id} for SSF
     * 1.0, or nested under {@code events.<type>.subject} for legacy
     * SSE CAEP), and the event body. Rendered as formatted JSON in
     * the lookup result so an operator can inspect exactly what the
     * receiver will see. Null when the encoded SET could not be
     * decoded.
     */
    @JsonProperty("decodedSet")
    private Map<String, Object> decodedSet;

    /**
     * Resolved Keycloak user UUID for the user the SET is about.
     * Null when the subject is org-only, resolves to no user, or the
     * subject format isn't user-identifying. Gives the admin UI a
     * click-through to the user detail page from the Pending Events
     * lookup result without requiring another search.
     */
    @JsonProperty("userId")
    private String userId;

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Long nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Long getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Long deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public Map<String, Object> getDecodedSet() {
        return decodedSet;
    }

    public void setDecodedSet(Map<String, Object> decodedSet) {
        this.decodedSet = decodedSet;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
