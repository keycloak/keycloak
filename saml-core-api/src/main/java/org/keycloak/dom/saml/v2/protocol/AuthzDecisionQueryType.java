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

import org.keycloak.dom.saml.v2.assertion.ActionType;
import org.keycloak.dom.saml.v2.assertion.EvidenceType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for AuthzDecisionQueryType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthzDecisionQueryType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}SubjectQueryAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Action" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Evidence" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Resource" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AuthzDecisionQueryType extends SubjectQueryAbstractType {

    protected List<ActionType> action = new ArrayList<>();

    protected EvidenceType evidence;

    protected URI resource;

    public AuthzDecisionQueryType(String id, XMLGregorianCalendar instant) {
        super(id, instant);
    }

    /**
     * Add an action
     *
     * @param act
     */
    public void addAction(ActionType act) {
        this.action.add(act);
    }

    /**
     * Remove an action
     *
     * @param act
     */
    public void removeAction(ActionType act) {
        this.action.remove(act);
    }

    /**
     * Gets the value of the action property.
     */
    public List<ActionType> getAction() {
        return Collections.unmodifiableList(this.action);
    }

    /**
     * Gets the value of the evidence property.
     *
     * @return possible object is {@link EvidenceType }
     */
    public EvidenceType getEvidence() {
        return evidence;
    }

    /**
     * Sets the value of the evidence property.
     *
     * @param value allowed object is {@link EvidenceType }
     */
    public void setEvidence(EvidenceType value) {
        this.evidence = value;
    }

    /**
     * Gets the value of the resource property.
     *
     * @return possible object is {@link String }
     */
    public URI getResource() {
        return resource;
    }

    /**
     * Sets the value of the resource property.
     *
     * @param value allowed object is {@link String }
     */
    public void setResource(URI value) {
        this.resource = value;
    }
}