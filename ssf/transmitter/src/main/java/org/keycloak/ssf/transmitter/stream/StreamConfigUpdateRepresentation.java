package org.keycloak.ssf.transmitter.stream;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire-format DTO for the request body of {@code PATCH /streams} (SSF spec
 * §8.1.1.3, merge semantics) and {@code PUT /streams} (§8.1.1.4, replace
 * semantics).
 *
 * <p>Adds {@code stream_id} to {@link StreamConfigInputRepresentation} so
 * the caller can identify the existing stream to update. Everything else —
 * the receiver-writable fields and the {@code mergeInto}/{@code replaceInto}
 * helpers — is inherited from {@link StreamConfigInputRepresentation}.
 * Transmitter-controlled fields are absent by design; Jackson rejects them
 * at bind time with 400 via {@code FAIL_ON_UNKNOWN_PROPERTIES}.
 */
public class StreamConfigUpdateRepresentation extends StreamConfigInputRepresentation {

    @JsonProperty("stream_id")
    protected String streamId;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }
}
