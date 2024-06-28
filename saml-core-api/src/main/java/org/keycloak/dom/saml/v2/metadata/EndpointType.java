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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for EndpointType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EndpointType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *       &lt;attribute name="Binding" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="Location" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *       &lt;attribute name="ResponseLocation" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class EndpointType extends TypeWithOtherAttributes {

    protected List<Object> any = new ArrayList<>();

    protected URI binding;

    protected URI location;

    protected URI responseLocation;

    public EndpointType(URI binding, URI location) {
        this.binding = binding;
        this.location = location;
    }

    /**
     * Add an object
     *
     * @param obj
     */
    public void addObject(Object obj) {
        this.any.add(obj);
    }

    /**
     * remove an object
     *
     * @param obj
     */
    public void removeObject(Object obj) {
        this.any.remove(obj);
    }

    /**
     * Gets the value of the any property.
     * <p>
     * Objects of the following type(s) are allowed in the list {@link org.w3c.dom.Element } {@link Object }
     */
    public List<Object> getAny() {
        return Collections.unmodifiableList(this.any);
    }

    /**
     * Gets the value of the binding property.
     *
     * @return possible object is {@link URI }
     */
    public URI getBinding() {
        return binding;
    }

    /**
     * Gets the value of the location property.
     *
     * @return possible object is {@link URI }
     */
    public URI getLocation() {
        return location;
    }

    /**
     * Gets the value of the responseLocation property.
     *
     * @return possible object is {@link URI }
     */
    public URI getResponseLocation() {
        return responseLocation;
    }

    /**
     * Sets the value of the responseLocation property.
     *
     * @param value allowed object is {@link URI }
     */
    public void setResponseLocation(URI value) {
        this.responseLocation = value;
    }
}