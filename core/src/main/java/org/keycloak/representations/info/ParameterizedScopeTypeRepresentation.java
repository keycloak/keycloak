package org.keycloak.representations.info;

public class ParameterizedScopeTypeRepresentation {

    private String name;
    private boolean repeatable;

    public ParameterizedScopeTypeRepresentation() {
    }

    public ParameterizedScopeTypeRepresentation(String name, boolean repeatable) {
        this.name = name;
        this.repeatable = repeatable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
}
