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
package org.keycloak.dom.saml.v2.assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for AuthzDecisionStatementType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthzDecisionStatementType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}StatementAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Action" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Evidence" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Resource" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="Decision" use="required" type="{urn:oasis:names:tc:SAML:2.0:assertion}DecisionType" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AuthzDecisionStatementType extends StatementAbstractType {

    protected List<ActionType> action = new ArrayList<>();
    protected EvidenceType evidence;
    protected String resource;
    protected DecisionType decision;

    /**
     * Get the list of actions (read-only list)
     *
     * @return {@link List} read only
     */
    public List<ActionType> getAction() {
        return Collections.unmodifiableList(this.action);
    }

    /**
     * Add an action
     *
     * @param actionType
     */
    public void addAction(ActionType actionType) {
        action.add(actionType);
    }

    /**
     * Remove an action
     *
     * @param actionType
     */
    public void removeAction(ActionType actionType) {
        action.remove(actionType);
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
    public String getResource() {
        return resource;
    }

    /**
     * Sets the value of the resource property.
     *
     * @param value allowed object is {@link String }
     */
    public void setResource(String value) {
        this.resource = value;
    }

    /**
     * Gets the value of the decision property.
     *
     * @return possible object is {@link DecisionType }
     */
    public DecisionType getDecision() {
        return decision;
    }

    /**
     * Sets the value of the decision property.
     *
     * @param value allowed object is {@link DecisionType }
     */
    public void setDecision(DecisionType value) {
        this.decision = value;
    }
}