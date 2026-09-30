package org.keycloak.scim.resource.bulk;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkResponse {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkResponse";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("Operations")
    private List<Operation> operations;

    public BulkResponse() {
        // reflection
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas != null ? schemas : Set.of();
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Operation {

        @JsonProperty("status")
        private String status;

        @JsonProperty("location")
        private String location;

        @JsonProperty("response")
        private JsonNode response;

        @JsonProperty("method")
        private String method;

        @JsonProperty("bulkId")
        private String bulkId;

        @JsonProperty("version")
        private String version;

        public Operation() {
            // reflection
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public JsonNode getResponse() {
            return response;
        }

        public void setResponse(JsonNode response) {
            this.response = response;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getBulkId() {
            return bulkId;
        }

        public void setBulkId(String bulkId) {
            this.bulkId = bulkId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
