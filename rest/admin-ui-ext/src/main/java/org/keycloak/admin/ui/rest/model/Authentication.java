package org.keycloak.admin.ui.rest.model;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class Authentication {

    @Schema(required = true)
    private String id;

    @Schema(required = true)
    private String alias;

    @Schema(required = true)
    private boolean builtIn;

    private UsedBy usedBy;

    private String description;

    public  UsedBy getUsedBy() {
        return usedBy;
    }

    public void setUsedBy( UsedBy usedBy) {
        this.usedBy = usedBy;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Authentication that = (Authentication) o;
        return builtIn == that.builtIn && Objects.equals(usedBy, that.usedBy) && Objects.equals(id, that.id) && Objects.equals(alias,
                that.alias) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usedBy, id, builtIn, alias, description);
    }

    @Override public String toString() {
        return "Authentication{" + "usedBy=" + usedBy + ", id='" + id + '\'' + ", buildIn=" + builtIn + ", alias='" + alias + '\'' + ", description='" + description + '\'' + '}';
    }
}
