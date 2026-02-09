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
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <complexType name="AuthenticationStatementType"> <complexContent> <extension base="saml:SubjectStatementAbstractType">
 *
 * <sequence> <element ref="saml:SubjectLocality" minOccurs="0"/> <element ref="saml:AuthorityBinding" minOccurs="0"
 * maxOccurs="unbounded"/> </sequence> <attribute name="AuthenticationMethod" type="anyURI" use="required"/> <attribute
 * name="AuthenticationInstant" type="dateTime" use="required"/> </extension> </complexContent> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AuthenticationStatementType extends SAML11SubjectStatementType {

    protected URI authenticationMethod;

    protected XMLGregorianCalendar authenticationInstant;

    protected SAML11SubjectLocalityType subjectLocality;

    protected List<SAML11AuthorityBindingType> authorityBinding = new ArrayList<>();

    public SAML11AuthenticationStatementType(URI authenticationMethod, XMLGregorianCalendar authenticationInstant) {
        this.authenticationMethod = authenticationMethod;
        this.authenticationInstant = authenticationInstant;
    }

    public URI getAuthenticationMethod() {
        return authenticationMethod;
    }

    public XMLGregorianCalendar getAuthenticationInstant() {
        return authenticationInstant;
    }

    public SAML11SubjectLocalityType getSubjectLocality() {
        return subjectLocality;
    }

    public void setSubjectLocality(SAML11SubjectLocalityType subjectLocality) {
        this.subjectLocality = subjectLocality;
    }

    public void add(SAML11AuthorityBindingType advice) {
        this.authorityBinding.add(advice);
    }

    public void addAllAuthorityBindingType(List<SAML11AuthorityBindingType> advice) {
        this.authorityBinding.addAll(advice);
    }

    public boolean remove(SAML11AuthorityBindingType advice) {
        return this.authorityBinding.remove(advice);
    }

    public List<SAML11AuthorityBindingType> getAuthorityBindingType() {
        return Collections.unmodifiableList(authorityBinding);
    }
}