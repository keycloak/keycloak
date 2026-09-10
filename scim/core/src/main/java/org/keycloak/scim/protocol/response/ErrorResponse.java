package org.keycloak.scim.protocol.response;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("status")
    private String status;

    @JsonProperty("scimType")
    private String scimType;

    @JsonProperty("detail")
    private String detail;

    public ErrorResponse() {
        // for reflection
    }

    public ErrorResponse(String detail, int status) {
        this.detail = detail;
        this.status = Integer.toString(status);
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public String getStatus() {
        return status;
    }

    @JsonIgnore
    public int getStatusInt() {
        return status == null ? -1 : Integer.parseInt(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScimType() {
        return scimType;
    }

    public void setScimType(String scimType) {
        this.scimType = scimType;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
