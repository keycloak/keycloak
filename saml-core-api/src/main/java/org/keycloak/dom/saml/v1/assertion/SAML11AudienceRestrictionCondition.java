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
 * <complexType name="AudienceRestrictionConditionType"> <complexContent> <extension base="saml:ConditionAbstractType">
 * <sequence> <element ref="saml:Audience" maxOccurs="unbounded"/> </sequence>
 *
 * </extension> </complexContent> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AudienceRestrictionCondition extends SAML11ConditionAbstractType {

    protected List<URI> audience = new ArrayList<>();

    public void add(URI advice) {
        this.audience.add(advice);
    }

    public void addAll(List<URI> advice) {
        this.audience.addAll(advice);
    }

    public boolean remove(URI advice) {
        return this.audience.remove(advice);
    }

    public List<URI> get() {
        return Collections.unmodifiableList(audience);
    }
}