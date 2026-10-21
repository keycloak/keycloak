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
package org.keycloak.dom.saml.v2.protocol;

import java.net.URI;

/**
 * <p>
 * Java class for NameIDPolicyType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="NameIDPolicyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="Format" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="SPNameQualifier" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="AllowCreate" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class NameIDPolicyType {

    protected URI format;
    protected String spNameQualifier;
    protected Boolean allowCreate = Boolean.FALSE;

    /**
     * Gets the value of the format property.
     *
     * @return possible object is {@link String }
     */
    public URI getFormat() {
        return format;
    }

    /**
     * Sets the value of the format property.
     *
     * @param value allowed object is {@link String }
     */
    public void setFormat(URI value) {
        this.format = value;
    }

    /**
     * Gets the value of the spNameQualifier property.
     *
     * @return possible object is {@link String }
     */
    public String getSPNameQualifier() {
        return spNameQualifier;
    }

    /**
     * Sets the value of the spNameQualifier property.
     *
     * @param value allowed object is {@link String }
     */
    public void setSPNameQualifier(String value) {
        this.spNameQualifier = value;
    }

    /**
     * Gets the value of the allowCreate property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isAllowCreate() {
        return allowCreate;
    }

    /**
     * Sets the value of the allowCreate property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setAllowCreate(Boolean value) {
        this.allowCreate = value;
    }
}