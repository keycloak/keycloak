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
package org.keycloak.dom.saml.v1.protocol;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.dom.saml.v1.assertion.SAML11ActionType;
import org.keycloak.dom.saml.v1.assertion.SAML11EvidenceType;

/**
 * <complexType name="AuthorizationDecisionQueryType"> <complexContent> <extension
 * base="samlp:SubjectQueryAbstractType">
 * <sequence>
 *
 * <element ref="saml:Action" maxOccurs="unbounded"/> <element ref="saml:Evidence" minOccurs="0"/> </sequence>
 * <attribute
 * name="Resource" type="anyURI" use="required"/> </extension> </complexContent> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AuthorizationDecisionQueryType extends SAML11SubjectQueryAbstractType {

    protected List<SAML11ActionType> action = new ArrayList<>();

    protected SAML11EvidenceType evidence;

    protected URI resource;

    public URI getResource() {
        return resource;
    }

    public void setResource(URI resource) {
        this.resource = resource;
    }

    public SAML11EvidenceType getEvidence() {
        return evidence;
    }

    public void setEvidence(SAML11EvidenceType evidence) {
        this.evidence = evidence;
    }

    public void add(SAML11ActionType sadt) {
        this.action.add(sadt);
    }

    public boolean remove(SAML11ActionType sadt) {
        return this.action.remove(sadt);
    }

    public List<SAML11ActionType> get() {
        return Collections.unmodifiableList(action);
    }
}