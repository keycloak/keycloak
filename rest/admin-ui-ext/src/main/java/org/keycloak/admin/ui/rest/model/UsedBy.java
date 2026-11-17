package org.keycloak.admin.ui.rest.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

public class UsedBy {
    public UsedBy(UsedByType type, List<String> values) {
        this(type, values, null);
    }

    public UsedBy(UsedByType type, List<String> values, List<UsedByClientRef> clientRefs) {
        this.type = type;
        this.values = values;
        this.clientRefs = clientRefs;
    }

    public enum UsedByType {
        SPECIFIC_CLIENTS, SPECIFIC_PROVIDERS, DEFAULT
    }

    private UsedByType type;
    private List<String> values;

    /**
     * Populated for {@link UsedByType#SPECIFIC_CLIENTS} flows so the admin UI can link to client settings without resolving clientId to internal id client-side.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<UsedByClientRef> clientRefs;

    public UsedByType getType() {
        return type;
    }

    public void setType(UsedByType type) {
        this.type = type;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public List<UsedByClientRef> getClientRefs() {
        return clientRefs;
    }

    public void setClientRefs(List<UsedByClientRef> clientRefs) {
        this.clientRefs = clientRefs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UsedBy usedBy = (UsedBy) o;
        return type == usedBy.type && Objects.equals(values, usedBy.values)
                && Objects.equals(clientRefs, usedBy.clientRefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, values, clientRefs);
    }
}
