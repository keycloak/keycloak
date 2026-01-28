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

package org.keycloak.adapters.saml;

import java.io.Serializable;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;

import org.w3c.dom.Document;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlPrincipal implements Serializable, Principal {

    public static final String DEFAULT_ROLE_ATTRIBUTE_NAME = "Roles";

    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    private String name;
    private String samlSubject;
    private String nameIDFormat;
    private AssertionType assertion;
    private Document assertionDocument;

    public SamlPrincipal(AssertionType assertion, String name, String samlSubject, String nameIDFormat, MultivaluedHashMap<String, String> attributes, MultivaluedHashMap<String, String> friendlyAttributes) {
        this(assertion, null, name, samlSubject, nameIDFormat, attributes, friendlyAttributes);
    }

    public SamlPrincipal(AssertionType assertion, Document assertionDocument, String name, String samlSubject, String nameIDFormat, MultivaluedHashMap<String, String> attributes, MultivaluedHashMap<String, String> friendlyAttributes) {
        this.name = name;
        this.attributes = attributes;
        this.friendlyAttributes = friendlyAttributes;
        this.samlSubject = samlSubject;
        this.nameIDFormat = nameIDFormat;
        this.assertion = assertion;
        this.assertionDocument = assertionDocument;
    }

    public SamlPrincipal() {
    }

    /**
     * Get full saml assertion
     *
     * @return
     */
    public AssertionType getAssertion() {
        return assertion;
    }

    /**
     * Get SAML subject sent in assertion
     *
     * @return
     */
    public String getSamlSubject() {
        return samlSubject;
    }

    /**
     * Subject nameID format
     *
     * @return
     */
    public String getNameIDFormat() {
        return nameIDFormat;
    }

    /**
     * Subject nameID format
     *
     * @return
     */
    public NameIDType getNameID() {
        if (assertion != null
          && assertion.getSubject() != null
          && assertion.getSubject().getSubType() != null
          && assertion.getSubject().getSubType().getBaseID() instanceof NameIDType) {
            return (NameIDType) assertion.getSubject().getSubType().getBaseID();
        }

        NameIDType res = new NameIDType();
        res.setValue(getSamlSubject());
        if (getNameIDFormat() != null) {
            res.setFormat(URI.create(getNameIDFormat()));
        }
        return res;
    }

    /*
     * The assertion element in DOM format, to respect the original syntax.
     * It's only available if option <em>keepDOMAssertion</em> is set to true.
     *
     * @return The document assertion or null
     */
    public Document getAssertionDocument() {
        return assertionDocument;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Convenience function that gets Attribute value by attribute name
     *
     * @param name
     * @return
     */
    public List<String> getAttributes(String name) {
        List<String> list = attributes.get(name);
        if (list != null) {
            return Collections.unmodifiableList(list);
        } else {
            return Collections.emptyList();
        }

    }

    /**
     * Convenience function that gets the attributes associated with this principal
     *
     * @return attributes associated with this principal
     */
    public Map<String, List<String>> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /**
     * Convenience function that gets Attribute value by attribute friendly name
     *
     * @param friendlyName
     * @return
     */
    public List<String> getFriendlyAttributes(String friendlyName) {
        List<String> list = friendlyAttributes.get(friendlyName);
        if (list != null) {
            return Collections.unmodifiableList(list);
        } else {
            return Collections.emptyList();
        }

    }

    /**
     * Convenience function that gets first  value of an attribute by attribute name
     *
     * @param name
     * @return
     */
    public String getAttribute(String name) {
        return attributes.getFirst(name);
    }

    /**
     * Convenience function that gets first  value of an attribute by attribute name
     *
     *
     * @param friendlyName
     * @return
     */
    public String getFriendlyAttribute(String friendlyName) {
        return friendlyAttributes.getFirst(friendlyName);
    }

    /**
     * Get set of all assertion attribute names
     *
     * @return
     */
    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());

    }

    /**
     * Get set of all assertion friendly attribute names
     *
     * @return
     */
    public Set<String> getFriendlyNames() {
        return Collections.unmodifiableSet(friendlyAttributes.keySet());

    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof SamlPrincipal))
            return false;

        SamlPrincipal otherPrincipal = (SamlPrincipal) other;

        return (this.name != null ? this.name.equals(otherPrincipal.name) : otherPrincipal.name == null) &&
                (this.samlSubject != null ? this.samlSubject.equals(otherPrincipal.samlSubject) : otherPrincipal.samlSubject == null) &&
                (this.nameIDFormat != null ? this.nameIDFormat.equals(otherPrincipal.nameIDFormat) : otherPrincipal.nameIDFormat == null) &&
                (this.attributes != null ? this.attributes.equals(otherPrincipal.attributes) : otherPrincipal.attributes == null) &&
                (this.friendlyAttributes != null ? this.friendlyAttributes.equals(otherPrincipal.friendlyAttributes) : otherPrincipal.friendlyAttributes == null);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.samlSubject == null ? 0 : this.samlSubject.hashCode());
        result = prime * result + (this.nameIDFormat == null ? 0 : this.nameIDFormat.hashCode());
        result = prime * result + (this.attributes == null ? 0 : this.attributes.hashCode());
        result = prime * result + (this.friendlyAttributes == null ? 0 : this.friendlyAttributes.hashCode());
        return result;
    }
}
