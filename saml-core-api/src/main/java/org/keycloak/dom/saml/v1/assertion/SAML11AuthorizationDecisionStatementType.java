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
package org.keycloak.dom.saml.v1.assertion;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <complexType name="AuthorizationDecisionStatementType"> <complexContent> <extension
 * base="saml:SubjectStatementAbstractType">
 * <sequence> <element ref="saml:Action" maxOccurs="unbounded"/> <element ref="saml:Evidence" minOccurs="0"/>
 *
 * </sequence> <attribute name="Resource" type="anyURI" use="required"/> <attribute name="Decision"
 * type="saml:DecisionType"
 * use="required"/> </extension> </complexContent> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AuthorizationDecisionStatementType extends SAML11SubjectStatementType {

    protected List<SAML11ActionType> actions = new ArrayList<>();

    protected SAML11EvidenceType evidence;

    protected URI resource;

    protected SAML11DecisionType decision;

    public SAML11AuthorizationDecisionStatementType(URI resource, SAML11DecisionType decision) {
        this.resource = resource;
        this.decision = decision;
    }

    public URI getResource() {
        return resource;
    }

    public SAML11DecisionType getDecision() {
        return decision;
    }

    public void addAction(SAML11ActionType action) {
        this.actions.add(action);
    }

    public boolean removeAction(SAML11ActionType action) {
        return this.actions.remove(action);
    }

    public List<SAML11ActionType> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public SAML11EvidenceType getEvidence() {
        return evidence;
    }

    public void setEvidence(SAML11EvidenceType evidence) {
        this.evidence = evidence;
    }
}