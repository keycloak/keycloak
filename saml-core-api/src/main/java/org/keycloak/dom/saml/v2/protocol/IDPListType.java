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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for IDPListType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="IDPListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}IDPEntry" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}GetComplete" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class IDPListType {

    protected List<IDPEntryType> idpEntry = new ArrayList<>();
    protected URI getComplete;

    /**
     * Add an idp entry
     *
     * @param entry
     */
    public void addIDPEntry(IDPEntryType entry) {
        this.idpEntry.add(entry);
    }

    /**
     * Remove an idp entry
     *
     * @param entry
     */
    public void removeIDPEntry(IDPEntryType entry) {
        this.idpEntry.remove(entry);
    }

    /**
     * Gets the value of the idpEntry property.
     */
    public List<IDPEntryType> getIDPEntry() {
        return Collections.unmodifiableList(this.idpEntry);
    }

    /**
     * Gets the value of the getComplete property.
     *
     * @return possible object is {@link String }
     */
    public URI getGetComplete() {
        return getComplete;
    }

    /**
     * Sets the value of the getComplete property.
     *
     * @param value allowed object is {@link String }
     */
    public void setGetComplete(URI value) {
        this.getComplete = value;
    }

}