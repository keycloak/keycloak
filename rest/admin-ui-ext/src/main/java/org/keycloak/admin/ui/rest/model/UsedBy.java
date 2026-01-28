package org.keycloak.admin.ui.rest.model;

import java.util.List;
import java.util.Objects;

public class UsedBy {
    public UsedBy(UsedByType type, List<String> values) {
        this.type = type;
        this.values = values;
    }

    public enum UsedByType {
        SPECIFIC_CLIENTS, SPECIFIC_PROVIDERS, DEFAULT
    }

    private UsedByType type;
    private List<String> values;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UsedBy usedBy = (UsedBy) o;
        return type == usedBy.type && Objects.equals(values, usedBy.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, values);
    }
}
