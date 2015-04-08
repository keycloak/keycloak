package org.keycloak.federation.ldap.idm.model;

import java.io.Serializable;
import java.util.Collection;

import org.keycloak.federation.ldap.idm.query.AttributeParameter;
import org.keycloak.federation.ldap.idm.query.QueryParameter;

/**
 *
 * @author Shane Bryzak
 *
 */
public interface AttributedType extends Serializable {

    /**
     * A query parameter used to set the id value.
     */
    QueryParameter ID = new AttributeParameter("id");

    /**
     * Returns the unique identifier for this instance
     * @return
     */
    String getId();

    /**
     * Sets the unique identifier for this instance
     * @return
     */
    void setId(String id);

    /**
     * Set the specified attribute. This operation will overwrite any previous value.
     *
     * @param attribute to be set
     */
    void setAttribute(Attribute<? extends Serializable> attribute);

    /**
     * Remove the attribute with given name
     *
     * @param name of attribute
     */
    void removeAttribute(String name);


    // LDAP specific stuff
    void setEntryDN(String entryDN);
    String getEntryDN();


    /**
     * Return the attribute value with the specified name
     *
     * @param name of attribute
     * @return attribute value or null if attribute with given name doesn't exist. If given attribute has many values method
     *         will return first one
     */
    <T extends Serializable> Attribute<T> getAttribute(String name);

    /**
     * Returns a Map containing all attribute values for this IdentityType instance.
     *
     * @return map of attribute names and their values
     */
    Collection<Attribute<? extends Serializable>> getAttributes();

    public final class QUERY_ATTRIBUTE {
        public static AttributeParameter byName(String name) {
            return new AttributeParameter(name);
        }
    }
}

