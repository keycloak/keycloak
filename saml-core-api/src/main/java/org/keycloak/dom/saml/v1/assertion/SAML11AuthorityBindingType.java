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
import javax.xml.namespace.QName;

/**
 * <complexType name="AuthorityBindingType"> <attribute name="AuthorityKind" type="QName" use="required"/> <attribute
 * name="Location" type="anyURI" use="required"/>
 *
 * <attribute name="Binding" type="anyURI" use="required"/> </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11AuthorityBindingType {

    protected QName authorityKind;

    protected URI location;

    protected URI binding;

    public SAML11AuthorityBindingType(QName authorityKind, URI location, URI binding) {
        super();
        this.authorityKind = authorityKind;
        this.location = location;
        this.binding = binding;
    }

    public QName getAuthorityKind() {
        return authorityKind;
    }

    public URI getLocation() {
        return location;
    }

    public URI getBinding() {
        return binding;
    }
}