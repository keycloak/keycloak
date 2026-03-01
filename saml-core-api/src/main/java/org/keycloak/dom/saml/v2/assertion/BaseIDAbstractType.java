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
package org.keycloak.dom.saml.v2.assertion;

import java.io.Serializable;

/**
 * Abstract Type that represents an ID
 *
 * <pre>
 *  &lt;attributeGroup name="IDNameQualifiers">
 *         &lt;attribute name="NameQualifier" type="string" use="optional"/>
 *         &lt;attribute name="SPNameQualifier" type="string" use="optional"/>
 *     &lt;/attributeGroup>
 *
 *     &lt;complexType name="BaseIDAbstractType" abstract="true">
 *         &lt;attributeGroup ref="saml:IDNameQualifiers"/>
 *     &lt;/complexType>
 * </pre>
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 24, 2010
 */
public abstract class BaseIDAbstractType implements Serializable {

    private String nameQualifier;
    private String sPNameQualifier;

    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }

    public String getSPNameQualifier() {
        return sPNameQualifier;
    }

    public void setSPNameQualifier(String sPNameQualifier) {
        this.sPNameQualifier = sPNameQualifier;
    }
}