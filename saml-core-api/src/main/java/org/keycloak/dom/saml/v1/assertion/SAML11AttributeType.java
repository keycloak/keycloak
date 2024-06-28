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
 * <complexType name="AttributeType"> <complexContent> <extension base="saml:AttributeDesignatorType"> <sequence>
 * <element
 * ref="saml:AttributeValue" maxOccurs="unbounded"/> </sequence> </extension> </complexContent>
 *
 * </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AttributeType extends SAML11AttributeDesignatorType {

    protected List<Object> attributeValues = new ArrayList<>();

    public SAML11AttributeType(String attributeName, URI attributeNamespace) {
        super(attributeName, attributeNamespace);
    }

    public void add(Object attribValue) {
        this.attributeValues.add(attribValue);
    }

    public void addAll(List<Object> attribValueList) {
        this.attributeValues.addAll(attribValueList);
    }

    public boolean remove(Object attribVal) {
        return this.attributeValues.remove(attribVal);
    }

    public List<Object> get() {
        return Collections.unmodifiableList(attributeValues);
    }
}