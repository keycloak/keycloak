package org.keycloak.federation.ldap.idm.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPObject {

    private static final Logger logger = Logger.getLogger(LDAPObject.class);

    private String uuid;
    private LDAPDn dn;
    private String rdnAttributeName;

    private final List<String> objectClasses = new LinkedList<>();

    // NOTE: names of read-only attributes are lower-cased to avoid case sensitivity issues
    private final List<String> readOnlyAttributeNames = new LinkedList<>();

    private final Map<String, Object> attributes = new HashMap<>();

    // Copy of "attributes" containing lower-cased keys
    private final Map<String, Object> lowerCasedAttributes = new HashMap<>();


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public LDAPDn getDn() {
        return dn;
    }

    public void setDn(LDAPDn dn) {
        this.dn = dn;
    }

    public List<String> getObjectClasses() {
        return objectClasses;
    }

    public void setObjectClasses(Collection<String> objectClasses) {
        this.objectClasses.clear();
        this.objectClasses.addAll(objectClasses);
    }

    public List<String> getReadOnlyAttributeNames() {
        return readOnlyAttributeNames;
    }

    public void addReadOnlyAttributeName(String readOnlyAttribute) {
        readOnlyAttributeNames.add(readOnlyAttribute.toLowerCase());
    }

    public String getRdnAttributeName() {
        return rdnAttributeName;
    }

    public void setRdnAttributeName(String rdnAttributeName) {
        this.rdnAttributeName = rdnAttributeName;
    }

    public void setAttribute(String attributeName, Object attributeValue) {
        attributes.put(attributeName, attributeValue);
        lowerCasedAttributes.put(attributeName.toLowerCase(), attributeValue);
    }

    public Object getAttributeCaseInsensitive(String name) {
        return lowerCasedAttributes.get(name.toLowerCase());
    }

    public String getAttributeAsStringCaseInsensitive(String name) {
        Object attrValue = lowerCasedAttributes.get(name.toLowerCase());
        if (attrValue != null && !(attrValue instanceof String)) {
            logger.warnf("Expected String but attribute '%s' has value '%s' of type '%s' ", name, attrValue, attrValue.getClass().getName());

            if (attrValue instanceof Collection) {
                Collection<String> attrValues = (Collection<String>) attrValue;
                attrValue = attrValues.iterator().next();
                logger.warnf("Returning just first founded value '%s' from the collection", attrValue);
            }
        }

        return (String) attrValue;
    }


    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!getClass().isInstance(obj)) {
            return false;
        }

        LDAPObject other = (LDAPObject) obj;

        return getUuid() != null && other.getUuid() != null && getUuid().equals(other.getUuid());
    }

    @Override
    public int hashCode() {
        int result = getUuid() != null ? getUuid().hashCode() : 0;
        result = 31 * result + (getUuid() != null ? getUuid().hashCode() : 0);
        return result;
    }
}
