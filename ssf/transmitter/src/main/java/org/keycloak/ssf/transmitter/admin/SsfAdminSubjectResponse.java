package org.keycloak.ssf.transmitter.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SsfAdminSubjectResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("entity_id")
    private String entityId;

    /**
     * Alias of the organization whose membership tipped the decision
     * for the {@code notified_via_org} / {@code ignored_via_org}
     * states; {@code null} for every other state. Lets the admin UI
     * render the source membership when explaining the result.
     */
    @JsonProperty("source_org_alias")
    private String sourceOrgAlias;

    public SsfAdminSubjectResponse() {
    }

    public SsfAdminSubjectResponse(String status, String entityType, String entityId) {
        this(status, entityType, entityId, null);
    }

    public SsfAdminSubjectResponse(String status, String entityType, String entityId, String sourceOrgAlias) {
        this.status = status;
        this.entityType = entityType;
        this.entityId = entityId;
        this.sourceOrgAlias = sourceOrgAlias;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSourceOrgAlias() {
        return sourceOrgAlias;
    }

    public void setSourceOrgAlias(String sourceOrgAlias) {
        this.sourceOrgAlias = sourceOrgAlias;
    }
}
