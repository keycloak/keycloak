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
 * Java class for IDPEntryType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="IDPEntryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="ProviderID" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="Name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Loc" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class IDPEntryType {

    protected URI providerID;
    protected String name;
    protected String loc;

    /**
     * Gets the value of the providerID property.
     *
     * @return possible object is {@link String }
     */
    public URI getProviderID() {
        return providerID;
    }

    /**
     * Sets the value of the providerID property.
     *
     * @param value allowed object is {@link String }
     */
    public void setProviderID(URI value) {
        this.providerID = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the loc property.
     *
     * @return possible object is {@link String }
     */
    public String getLoc() {
        return loc;
    }

    /**
     * Sets the value of the loc property.
     *
     * @param value allowed object is {@link String }
     */
    public void setLoc(String value) {
        this.loc = value;
    }

}