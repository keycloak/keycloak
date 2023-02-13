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
package org.keycloak.dom.xmlsec.w3.xmldsig;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for TransformType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="TransformType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice maxOccurs="unbounded" minOccurs="0">
 *         &lt;any/>
 *         &lt;element name="XPath" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/choice>
 *       &lt;attribute name="Algorithm" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class TransformType {

    protected List<Object> content = new ArrayList<>();
    protected URI algorithm;

    public TransformType(URI algorithm) {
        this.algorithm = algorithm;
    }

    public void addTransform(Object obj) {
        this.content.add(obj);
    }

    public void removeTransform(Object obj) {
        this.content.remove(obj);
    }

    /**
     * Gets the value of the content property.
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link org.w3c.dom.Element } {@link String } {@link Object }
     */
    public List<Object> getContent() {
        return Collections.unmodifiableList(this.content);
    }

    /**
     * Gets the value of the algorithm property.
     *
     * @return possible object is {@link String }
     */
    public URI getAlgorithm() {
        return algorithm;
    }

}