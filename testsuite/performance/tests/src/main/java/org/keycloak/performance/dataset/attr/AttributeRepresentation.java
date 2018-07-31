package org.keycloak.performance.dataset.attr;

/**
 *
 * @author tkyjovsk
 */
public abstract class AttributeRepresentation<V> {

    private String name;
    private V value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

}
