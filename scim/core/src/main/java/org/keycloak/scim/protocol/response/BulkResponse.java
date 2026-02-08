package org.keycloak.scim.protocol.response;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkResponse {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkResponse";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("Operations")
    private List<BulkResponseOperation> operations;

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public List<BulkResponseOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<BulkResponseOperation> operations) {
        this.operations = operations;
    }

    /**
     * Represents a single bulk operation response
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkResponseOperation {
        @JsonProperty("location")
        private String location;

        @JsonProperty("method")
        private String method;

        @JsonProperty("bulkId")
        private String bulkId;

        @JsonProperty("version")
        private String version;

        @JsonProperty("status")
        private String status;

        @JsonProperty("response")
        private Map<String, Object> response;

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getResponse() {
            return response;
        }

        public void setResponse(Map<String, Object> response) {
            this.response = response;
        }
    }
}
