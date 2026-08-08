package org.keycloak.representations.admin.v2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class BaseRepresentation {

    // Hard requirement for all representations to support nullifying field in PATCH merge
    @JsonIgnore
    protected Map<String, Object> additionalFields = new LinkedHashMap<String, Object>();

    @JsonIgnore
    private final Set<String> explicitlySetFields = new LinkedHashSet<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    @JsonAnySetter
    public void setAdditionalField(String name, Object value) {
        this.additionalFields.put(name, value);
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields;
    }

    protected void markFieldAsExplicitlySet(String fieldName) {
        explicitlySetFields.add(fieldName);
    }

    @JsonIgnore
    public boolean isFieldExplicitlySet(String fieldName) {
        return explicitlySetFields.contains(fieldName);
    }

    public void clearExplicitlySetFields() {
        explicitlySetFields.clear();
    }
}
