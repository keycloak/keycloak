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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.assertion.AssertionType;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlPrincipal implements Serializable, Principal {
    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private MultivaluedHashMap<String, String> friendlyAttributes = new MultivaluedHashMap<>();
    private String name;
    private String samlSubject;
    private String nameIDFormat;
    private AssertionType assertion;

    public SamlPrincipal(AssertionType assertion, String name, String samlSubject, String nameIDFormat, MultivaluedHashMap<String, String> attributes, MultivaluedHashMap<String, String> friendlyAttributes) {
        this.name = name;
        this.attributes = attributes;
        this.friendlyAttributes = friendlyAttributes;
        this.samlSubject = samlSubject;
        this.nameIDFormat = nameIDFormat;
        this.assertion = assertion;
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
     * Convenience function that gets Attribute value by attribute friendly name
     *
     * @param friendlyName
     * @return
     */
    public List<String> getFriendlyAttributes(String friendlyName) {
        List<String> list = friendlyAttributes.get(name);
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

}
