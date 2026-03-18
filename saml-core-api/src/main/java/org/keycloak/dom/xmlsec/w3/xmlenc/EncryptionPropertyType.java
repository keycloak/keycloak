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
package org.keycloak.dom.xmlsec.w3.xmlenc;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * <p>
 * Java class for EncryptionPropertyType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EncryptionPropertyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;any/>
 *       &lt;/choice>
 *       &lt;attribute name="Target" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="Id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class EncryptionPropertyType {

    protected URI target;
    protected String id;
    private Map<QName, String> otherAttributes = new HashMap<>();

    /**
     * Gets the value of the target property.
     *
     * @return possible object is {@link URI }
     */
    public URI getTarget() {
        return target;
    }

    /**
     * Sets the value of the target property.
     *
     * @param value allowed object is {@link URI }
     */
    public void setTarget(URI value) {
        this.target = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    public void setId(String value) {
        this.id = value;
    }

    public void addOtherAttribute(QName key, String val) {
        this.otherAttributes.put(key, val);
    }

    public void addOtherAttributes(Map<QName, String> otherMap) {
        this.otherAttributes.putAll(otherMap);
    }

    public void removeOtherAttribute(QName key) {
        this.otherAttributes.remove(key);
    }

    /**
     * Gets a map that contains attributes that aren't bound to any typed property on this class.
     *
     * @return always non-null
     */
    public Map<QName, String> getOtherAttributes() {
        return Collections.unmodifiableMap(otherAttributes);
    }
}