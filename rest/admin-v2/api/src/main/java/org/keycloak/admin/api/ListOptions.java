package org.keycloak.admin.api;

import java.util.Set;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListOptions {

    @Parameter(description = "Set of fields to include in the response. Must be top-level fields. If omitted or empty, all fields will be populated.")
    @QueryParam("fields")
    protected Set<String> fields;
    
    public ListOptions fields(Set<String> fields) {
        this.setFields(fields);
        return this;
    }

    public Set<String> getFields() {
        return fields;
    }
    
    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

}
