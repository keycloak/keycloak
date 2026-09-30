package org.keycloak.scim.resource.bulk;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkRequest {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkRequest";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("failOnErrors")
    private Integer failOnErrors;

    @JsonProperty("Operations")
    private List<Operation> operations;

    public BulkRequest() {
        // reflection
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas != null ? schemas : Set.of();
    }

    public Integer getFailOnErrors() {
        return failOnErrors;
    }

    public void setFailOnErrors(Integer failOnErrors) {
        this.failOnErrors = failOnErrors;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public void setOperations(List<Operation> operations) {
        this.operations = operations;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Operation {

        @JsonProperty("method")
        private String method;

        @JsonProperty("path")
        private String path;

        @JsonProperty("bulkId")
        private String bulkId;

        @JsonProperty("version")
        private String version;

        @JsonProperty("data")
        private JsonNode data;

        public Operation() {
            // reflection
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
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

        public JsonNode getData() {
            return data;
        }

        public void setData(JsonNode data) {
            this.data = data;
        }
    }
}
