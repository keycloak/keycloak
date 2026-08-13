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

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for ScopingType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ScopingType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}IDPList" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}RequesterID" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ProxyCount" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class ScopingType {

    protected IDPListType idpList;
    protected List<URI> requesterID = new ArrayList<>();

    protected BigInteger proxyCount;

    /**
     * Gets the value of the idpList property.
     *
     * @return possible object is {@link IDPListType }
     */
    public IDPListType getIDPList() {
        return idpList;
    }

    /**
     * Sets the value of the idpList property.
     *
     * @param value allowed object is {@link IDPListType }
     */
    public void setIDPList(IDPListType value) {
        this.idpList = value;
    }

    /**
     * Gets the value of the requesterID property.
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getRequesterID().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<URI> getRequesterID() {
        return Collections.unmodifiableList(this.requesterID);
    }

    /**
     * Add requester id
     *
     * @param uri
     */
    public void addRequesterID(URI uri) {
        this.requesterID.add(uri);
    }

    /**
     * Remove requester id
     *
     * @param uri
     */
    public void removeRequesterID(URI uri) {
        this.requesterID.remove(uri);
    }

    /**
     * Gets the value of the proxyCount property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getProxyCount() {
        return proxyCount;
    }

    /**
     * Sets the value of the proxyCount property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setProxyCount(BigInteger value) {
        this.proxyCount = value;
    }

}
