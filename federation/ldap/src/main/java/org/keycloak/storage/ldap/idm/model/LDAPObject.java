/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.storage.ldap.idm.model;

import org.jboss.logging.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPObject {

    private static final Logger logger = Logger.getLogger(LDAPObject.class);

    private String uuid;
    private LDAPDn dn;

    // In most cases, there is single "rdnAttributeName" . Usually "uid" or "cn"
    private final List<String> rdnAttributeNames = new LinkedList<>();

    private final List<String> objectClasses = new LinkedList<>();

    // NOTE: names of read-only attributes are lower-cased to avoid case sensitivity issues
    private final List<String> readOnlyAttributeNames = new LinkedList<>();

    private final Map<String, Set<String>> attributes = new HashMap<>();

    // Copy of "attributes" containing lower-cased keys
    private final Map<String, Set<String>> lowerCasedAttributes = new HashMap<>();

    // range attributes are always read from 0 to max so just saving the top value
    private final Map<String, Integer> rangedAttributes = new HashMap<>();

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

    public void removeReadOnlyAttributeName(String readOnlyAttribute) {
        readOnlyAttributeNames.remove(readOnlyAttribute.toLowerCase());
    }

    public List<String> getRdnAttributeNames() {
        return rdnAttributeNames;
    }

    /**
     * Useful when single value will be used as the "RDN" attribute. Which will be most of the cases
     */
    public void setRdnAttributeName(String rdnAttributeName) {
        this.rdnAttributeNames.clear();
        this.rdnAttributeNames.add(rdnAttributeName);
    }

    public void setRdnAttributeNames(List<String> rdnAttributeNames) {
        this.rdnAttributeNames.clear();
        this.rdnAttributeNames.addAll(rdnAttributeNames);
    }

    public void addRdnAttributeName(String rdnAttributeName) {
        this.rdnAttributeNames.add(rdnAttributeName);
    }

    public void setSingleAttribute(String attributeName, String attributeValue) {
        Set<String> asSet = new LinkedHashSet<>();
        asSet.add(attributeValue);
        setAttribute(attributeName, asSet);
    }

    public void setAttribute(String attributeName, Set<String> attributeValue) {
        attributes.put(attributeName, attributeValue);
        lowerCasedAttributes.put(attributeName.toLowerCase(), attributeValue);
    }

    // Case-insensitive
    public String getAttributeAsString(String name) {
        Set<String> attrValue = lowerCasedAttributes.get(name.toLowerCase());
        if (attrValue == null || attrValue.size() == 0) {
            return null;
        } else if (attrValue.size() > 1) {
            logger.warnf("Expected String but attribute '%s' has more values '%s' on object '%s' . Returning just first value", name, attrValue, dn);
        }

        return attrValue.iterator().next();
    }

    // Case-insensitive. Return null if there is not value of attribute with given name or set with all values otherwise
    public Set<String> getAttributeAsSet(String name) {
        Set<String> values = lowerCasedAttributes.get(name.toLowerCase());
        return (values == null) ? null : new LinkedHashSet<>(values);
    }

    public boolean isRangeComplete(String name) {
        return !rangedAttributes.containsKey(name);
    }

    public int getCurrentRange(String name) {
        return rangedAttributes.get(name);
    }

    public boolean isRangeCompleteForAllAttributes() {
        return rangedAttributes.isEmpty();
    }

    public void addRangedAttribute(String name, int max) {
        Integer current = rangedAttributes.get(name);
        if (current == null || max > current) {
            rangedAttributes.put(name, max);
        }
    }

    public void populateRangedAttribute(LDAPObject obj, String name) {
        Set<String> newValues = obj.getAttributes().get(name);
        if (newValues != null && attributes.containsKey(name)) {
            attributes.get(name).addAll(newValues);
            if (!obj.isRangeComplete(name)) {
                addRangedAttribute(name, obj.getCurrentRange(name));
            } else {
                rangedAttributes.remove(name);
            }
        }
    }

    public Map<String, Set<String>> getAttributes() {
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

    @Override
    public String toString() {
        return "LDAP Object [ dn: " + dn + " , uuid: " + uuid + ", attributes: " + attributes +
                ", readOnly attribute names: " + readOnlyAttributeNames + ", ranges: " + rangedAttributes + " ]";
    }
}
