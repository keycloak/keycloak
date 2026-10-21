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
package org.keycloak.dom.saml.v2.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for AuthnAuthorityDescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthnAuthorityDescriptorType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:metadata}RoleDescriptorType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AuthnQueryService" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}AssertionIDRequestService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}NameIDFormat" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AuthnAuthorityDescriptorType extends RoleDescriptorType {

    protected List<EndpointType> authnQueryService = new ArrayList<>();

    protected List<EndpointType> assertionIDRequestService = new ArrayList<>();

    protected List<String> nameIDFormat = new ArrayList<>();

    public AuthnAuthorityDescriptorType(List<String> protocolSupport) {
        super(protocolSupport);
    }

    /**
     * Add authn query service
     *
     * @param endpoint
     */
    public void addAuthnQueryService(EndpointType endpoint) {
        this.authnQueryService.add(endpoint);
    }

    /**
     * Add assertion id request service
     *
     * @param endpoint
     */
    public void addAssertionIDRequestService(EndpointType endpoint) {
        this.assertionIDRequestService.add(endpoint);
    }

    /**
     * Add name id format
     *
     * @param str
     */
    public void addNameIDFormat(String str) {
        this.nameIDFormat.add(str);
    }

    /**
     * Remove authn query service
     *
     * @param endpoint
     */
    public void removeAuthnQueryService(EndpointType endpoint) {
        this.authnQueryService.remove(endpoint);
    }

    /**
     * remove assertion id request service
     *
     * @param endpoint
     */
    public void removeAssertionIDRequestService(EndpointType endpoint) {
        this.assertionIDRequestService.remove(endpoint);
    }

    /**
     * remove name id format
     *
     * @param str
     */
    public void removeNameIDFormat(String str) {
        this.nameIDFormat.remove(str);
    }

    /**
     * Gets the value of the authnQueryService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getAuthnQueryService() {
        return Collections.unmodifiableList(this.authnQueryService);
    }

    /**
     * Gets the value of the assertionIDRequestService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getAssertionIDRequestService() {
        return Collections.unmodifiableList(this.assertionIDRequestService);
    }

    /**
     * Gets the value of the nameIDFormat property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     */
    public List<String> getNameIDFormat() {
        return Collections.unmodifiableList(this.nameIDFormat);
    }
}