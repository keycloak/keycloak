package org.keycloak.federation.ldap.idm.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;

/**
 * Abstract base class for all AttributedType implementations
 *
 * @author Shane Bryzak
 *
 */
public abstract class AbstractAttributedType implements AttributedType {
    private static final long serialVersionUID = -6118293036241099199L;

    private String id;
    private String entryDN;

    private Map<String, Attribute<? extends Serializable>> attributes =
            new HashMap<String, Attribute<? extends Serializable>>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntryDN() {
        return entryDN;
    }

    public void setEntryDN(String entryDN) {
        this.entryDN = entryDN;
    }

    public void setAttribute(Attribute<? extends Serializable> attribute) {
        attributes.put(attribute.getName(), attribute);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> Attribute<T> getAttribute(String name) {
        return (Attribute<T>) attributes.get(name);
    }

    public Collection<Attribute<? extends Serializable>> getAttributes() {
        return unmodifiableCollection(attributes.values());
    }

    public Map<String,Attribute<? extends Serializable>> getAttributesMap() {
        return unmodifiableMap(attributes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!getClass().isInstance(obj)) {
            return false;
        }

        AttributedType other = (AttributedType) obj;

        return getId() != null && other.getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

}