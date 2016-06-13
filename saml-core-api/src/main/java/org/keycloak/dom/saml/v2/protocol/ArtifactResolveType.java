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

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>
 * Java class for ArtifactResolveType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ArtifactResolveType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Artifact"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ArtifactResolveType extends RequestAbstractType {

    protected String artifact;

    public ArtifactResolveType(String id, XMLGregorianCalendar instant) {
        super(id, instant);
    }

    /**
     * Gets the value of the artifact property.
     *
     * @return possible object is {@link String }
     */
    public String getArtifact() {
        return artifact;
    }

    /**
     * Sets the value of the artifact property.
     *
     * @param value allowed object is {@link String }
     */
    public void setArtifact(String value) {
        this.artifact = value;
    }

}