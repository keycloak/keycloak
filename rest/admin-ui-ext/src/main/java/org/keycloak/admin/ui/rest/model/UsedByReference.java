package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

public class UsedByReference {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;
    private String label;

    public UsedByReference() {
    }

    public UsedByReference(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UsedByReference that = (UsedByReference) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label);
    }
}
