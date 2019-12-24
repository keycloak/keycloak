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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <complexType name="EvidenceType"> <choice maxOccurs="unbounded"> <element ref="saml:AssertionIDReference"/>
 *
 * <element ref="saml:Assertion"/> </choice> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11EvidenceType {

    protected List<String> assertionIDReference = new ArrayList<>();

    protected List<SAML11AssertionType> assertions = new ArrayList<>();

    public void add(String condition) {
        this.assertionIDReference.add(condition);
    }

    public void addAllAssertionIDReference(List<String> theassertionIDReference) {
        this.assertionIDReference.addAll(theassertionIDReference);
    }

    public boolean remove(String assertionIDReference) {
        return this.assertionIDReference.remove(assertionIDReference);
    }

    public List<String> getAssertionIDReference() {
        return Collections.unmodifiableList(assertionIDReference);
    }

    public void add(SAML11AssertionType condition) {
        this.assertions.add(condition);
    }

    public void addAllAssertionType(List<SAML11AssertionType> theassertions) {
        this.assertions.addAll(theassertions);
    }

    public boolean remove(SAML11AssertionType assertion) {
        return this.assertions.remove(assertionIDReference);
    }

    public List<SAML11AssertionType> getAssertions() {
        return Collections.unmodifiableList(assertions);
    }
}