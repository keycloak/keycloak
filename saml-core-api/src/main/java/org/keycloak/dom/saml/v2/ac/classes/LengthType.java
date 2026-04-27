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

package org.keycloak.dom.saml.v2.ac.classes;

import java.math.BigInteger;

/**
 * <p>
 * Java class for LengthType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="LengthType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="min" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class LengthType {

    protected BigInteger min;
    protected BigInteger max;

    /**
     * Gets the value of the min property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getMin() {
        return min;
    }

    /**
     * Sets the value of the min property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setMin(BigInteger value) {
        this.min = value;
    }

    /**
     * Gets the value of the max property.
     *
     * @return possible object is {@link BigInteger }
     */
    public BigInteger getMax() {
        return max;
    }

    /**
     * Sets the value of the max property.
     *
     * @param value allowed object is {@link BigInteger }
     */
    public void setMax(BigInteger value) {
        this.max = value;
    }

}
