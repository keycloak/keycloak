package org.keycloak.models.entities;

import org.keycloak.models.ClaimTypeModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClaimTypeEntity {
    protected String id;

    private String name;

    protected boolean builtIn;

    protected ClaimTypeModel.ValueType type;

    private String realmId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public ClaimTypeModel.ValueType getType() {
        return type;
    }

    public void setType(ClaimTypeModel.ValueType type) {
        this.type = type;
    }

}

