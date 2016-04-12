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

import java.io.Serializable;
import java.net.URI;

/**
 * <complexType name="NameIdentifierType"> <simpleContent> <extension base="string"> <attribute name="NameQualifier"
 * type="string" use="optional"/> <attribute name="Format" type="anyURI" use="optional"/> </extension> </simpleContent>
 * </complexType>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 22, 2011
 */
public class SAML11NameIdentifierType implements Serializable {

    protected String nameQualifier;

    protected URI format;

    protected String value;

    public SAML11NameIdentifierType(String val) {
        this.value = val;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }

    public URI getFormat() {
        return format;
    }

    public void setFormat(URI format) {
        this.format = format;
    }

    public String getValue() {
        return value;
    }
}