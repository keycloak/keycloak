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
 * Java class for SSODescriptorType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SSODescriptorType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:metadata}RoleDescriptorType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}ArtifactResolutionService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}SingleLogoutService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}ManageNameIDService" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:metadata}NameIDFormat" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public abstract class SSODescriptorType extends RoleDescriptorType {

    protected List<IndexedEndpointType> artifactResolutionService = new ArrayList<>();

    protected List<EndpointType> singleLogoutService = new ArrayList<>();

    protected List<EndpointType> manageNameIDService = new ArrayList<>();

    protected List<String> nameIDFormat = new ArrayList<>();

    public SSODescriptorType(List<String> protocolSupport) {
        super(protocolSupport);
    }

    /**
     * Add SLO Service
     *
     * @param endpt
     */
    public void addSingleLogoutService(EndpointType endpt) {
        this.singleLogoutService.add(endpt);
    }

    /**
     * Add atrifact resolution service
     *
     * @param i
     */
    public void addArtifactResolutionService(IndexedEndpointType i) {
        this.artifactResolutionService.add(i);
    }

    /**
     * Add manage name id service
     *
     * @param end
     */
    public void addManageNameIDService(EndpointType end) {
        this.manageNameIDService.add(end);
    }

    /**
     * Add Name ID Format
     *
     * @param s
     */
    public void addNameIDFormat(String s) {
        this.nameIDFormat.add(s);
    }

    /**
     * remove SLO Service
     *
     * @param endpt
     */
    public void removeSingleLogoutService(EndpointType endpt) {
        this.singleLogoutService.remove(endpt);
    }

    /**
     * remove atrifact resolution service
     *
     * @param i
     */
    public void removeArtifactResolutionService(IndexedEndpointType i) {
        this.artifactResolutionService.remove(i);
    }

    /**
     * remove manage name id service
     *
     * @param end
     */
    public void removeManageNameIDService(EndpointType end) {
        this.manageNameIDService.remove(end);
    }

    /**
     * remove Name ID Format
     *
     * @param s
     */
    public void removeNameIDFormat(String s) {
        this.nameIDFormat.remove(s);
    }

    /**
     * Gets the value of the artifactResolutionService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link IndexedEndpointType }
     */
    public List<IndexedEndpointType> getArtifactResolutionService() {
        return Collections.unmodifiableList(this.artifactResolutionService);
    }

    /**
     * Gets the value of the singleLogoutService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getSingleLogoutService() {
        return Collections.unmodifiableList(this.singleLogoutService);
    }

    /**
     * Gets the value of the manageNameIDService property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EndpointType }
     */
    public List<EndpointType> getManageNameIDService() {
        return Collections.unmodifiableList(this.manageNameIDService);
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