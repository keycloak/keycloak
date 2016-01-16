package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClaimTypeModel {

    public static enum ValueType {
        BOOLEAN,
        INT,
        STRING
    }

    private String id;
    private String name;
    private boolean builtIn;
    private ValueType type;

    public ClaimTypeModel(ClaimTypeModel copy) {
        this(copy.getId(), copy.getName(), copy.isBuiltIn(), copy.getType());
    }

    public ClaimTypeModel(String id, String name, boolean builtIn, ValueType type) {
        this.id = id;
        this.name = name;
        this.builtIn = builtIn;
        this.type = type;
    }

    public ClaimTypeModel() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isBuiltIn() {
        return builtIn;
    }

    public ValueType getType() {
        return type;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBuiltIn(boolean builtIn) {
        this.builtIn = builtIn;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClaimTypeModel that = (ClaimTypeModel) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
