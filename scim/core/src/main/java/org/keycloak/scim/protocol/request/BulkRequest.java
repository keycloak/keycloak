package org.keycloak.scim.protocol.request;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkRequest {

    public static final String SCHEMA = "urn:ietf:params:scim:api:messages:2.0:BulkRequest";

    @JsonProperty("schemas")
    private Set<String> schemas = Set.of(SCHEMA);

    @JsonProperty("failOnErrors")
    private Integer failOnErrors;

    @JsonProperty("Operations")
    private List<BulkOperation> operations;

    public Set<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(Set<String> schemas) {
        this.schemas = schemas;
    }

    public Integer getFailOnErrors() {
        return failOnErrors;
    }

    public void setFailOnErrors(Integer failOnErrors) {
        this.failOnErrors = failOnErrors;
    }

    public List<BulkOperation> getOperations() {
        return operations;
    }

    public void setOperations(List<BulkOperation> operations) {
        this.operations = operations;
    }

    /**
     * Represents a single bulk operation
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BulkOperation {
        @JsonProperty("method")
        private String method; // "POST", "PUT", "PATCH", "DELETE"

        @JsonProperty("bulkId")
        private String bulkId;

        @JsonProperty("version")
        private String version;

        @JsonProperty("path")
        private String path;

        @JsonProperty("data")
        private Map<String, Object> data;

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

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}
