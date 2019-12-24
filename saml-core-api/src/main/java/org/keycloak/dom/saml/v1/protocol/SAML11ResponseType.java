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

import org.keycloak.dom.saml.v1.assertion.SAML11AssertionType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <complexType name="ResponseType"> <complexContent> <extension base="samlp:ResponseAbstractType"> <sequence> <element
 * ref="samlp:Status"/> <element ref="saml:Assertion" minOccurs="0" maxOccurs="unbounded"/> </sequence> </extension>
 *
 * </complexContent> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11ResponseType extends SAML11ResponseAbstractType {

    protected List<SAML11AssertionType> assertions = new ArrayList<>();

    protected SAML11StatusType status;

    public SAML11ResponseType(String id, XMLGregorianCalendar issueInstant) {
        super(id, issueInstant);
    }

    public void add(SAML11AssertionType assertion) {
        this.assertions.add(assertion);
    }

    public boolean remove(SAML11AssertionType assertion) {
        return this.assertions.remove(assertion);
    }

    public List<SAML11AssertionType> get() {
        return Collections.unmodifiableList(assertions);
    }

    public SAML11StatusType getStatus() {
        return status;
    }

    public void setStatus(SAML11StatusType status) {
        this.status = status;
    }
}