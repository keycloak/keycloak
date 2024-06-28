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

import org.w3c.dom.Element;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <complexType name="SubjectConfirmationType"> <sequence> <element ref="saml:ConfirmationMethod"
 * maxOccurs="unbounded"/>
 * <element ref="saml:SubjectConfirmationData" minOccurs="0"/>
 *
 * <element ref="ds:KeyInfo" minOccurs="0"/> </sequence> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11SubjectConfirmationType {

    protected List<URI> confirmationMethod = new ArrayList<>();

    protected Object subjectConfirmationData;

    protected Element keyInfo;

    public void addConfirmationMethod(URI confirmation) {
        this.confirmationMethod.add(confirmation);
    }

    public void addAllConfirmationMethod(List<URI> confirmation) {
        this.confirmationMethod.addAll(confirmation);
    }

    public boolean removeConfirmationMethod(URI confirmation) {
        return this.confirmationMethod.remove(confirmation);
    }

    public List<URI> getConfirmationMethod() {
        return Collections.unmodifiableList(confirmationMethod);
    }

    public void setSubjectConfirmationData(Object subjectConfirmation) {
        this.subjectConfirmationData = subjectConfirmation;
    }

    public Element getKeyInfo() {
        return keyInfo;
    }

    public void setKeyInfo(Element keyInfo) {
        this.keyInfo = keyInfo;
    }

    public Object getSubjectConfirmationData() {
        return subjectConfirmationData;
    }
}