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

import org.keycloak.dom.saml.common.CommonRequestAbstractType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <complexType name="RequestAbstractType" abstract="true">
 *
 * <sequence> <element ref="samlp:RespondWith" minOccurs="0" maxOccurs="unbounded"/> <element ref="ds:Signature"
 * minOccurs="0"/>
 * </sequence> <attribute name="RequestID" type="ID" use="required"/> <attribute name="MajorVersion" type="integer"
 * use="required"/> <attribute name="MinorVersion" type="integer" use="required"/> <attribute name="IssueInstant"
 * type="dateTime" use="required"/> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public abstract class SAML11RequestAbstractType extends CommonRequestAbstractType {

    protected int majorVersion = 1;

    protected int minorVersion = 1;

    protected List<QName> respondWith = new ArrayList<>();

    public SAML11RequestAbstractType(String id, XMLGregorianCalendar issueInstant) {
        super(id, issueInstant);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void add(QName rw) {
        this.respondWith.add(rw);
    }

    public void addAllConditions(List<QName> rw) {
        this.respondWith.addAll(rw);
    }

    public boolean remove(QName rw) {
        return this.respondWith.remove(rw);
    }

    public List<QName> getRespondWith() {
        return Collections.unmodifiableList(respondWith);
    }
}