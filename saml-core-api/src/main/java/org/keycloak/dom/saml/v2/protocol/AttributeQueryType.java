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
package org.keycloak.dom.saml.v2.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.keycloak.dom.saml.v2.assertion.AttributeType;

/**
 * <p>
 * Java class for AttributeQueryType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AttributeQueryType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}SubjectQueryAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AttributeQueryType extends SubjectQueryAbstractType {

    protected List<AttributeType> attribute = new ArrayList<>();

    public AttributeQueryType(String id, XMLGregorianCalendar instant) {
        super(id, instant);
    }

    /**
     * Add an attribute
     *
     * @param att
     */
    public void add(AttributeType att) {
        this.attribute.add(att);
    }

    /**
     * Remove an attribute
     *
     * @param att
     */
    public void remove(AttributeType att) {
        this.attribute.remove(att);
    }

    /**
     * Gets the value of the attribute property.
     */
    public List<AttributeType> getAttribute() {
        return Collections.unmodifiableList(this.attribute);
    }
}