package org.keycloak.ssf.transmitter.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a synthetic SSF event emission attempt. The caller (a trusted
 * IAM management client) sees the exact dispatch outcome so it can
 * distinguish a successful push from a filter-dropped event for
 * debugging.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SsfEmitEventResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("jti")
    private String jti;

    public SsfEmitEventResponse() {
    }

    public SsfEmitEventResponse(String status) {
        this.status = status;
    }

    public SsfEmitEventResponse(String status, String jti) {
        this.status = status;
        this.jti = jti;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }
}
