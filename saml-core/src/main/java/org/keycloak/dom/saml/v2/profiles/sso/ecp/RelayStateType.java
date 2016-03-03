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

package org.keycloak.dom.saml.v2.profiles.sso.ecp;

/**
 * <p>
 * Java class for RelayStateType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="RelayStateType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute ref="{http://schemas.xmlsoap.org/soap/envelope/}mustUnderstand use="required""/>
 *       &lt;attribute ref="{http://schemas.xmlsoap.org/soap/envelope/}actor use="required""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
public class RelayStateType {

    protected String value;
    protected Boolean mustUnderstand = Boolean.FALSE;
    protected String actor;

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is {@link String }
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the mustUnderstand property.
     *
     * @return possible object is {@link String }
     */
    public Boolean isMustUnderstand() {
        return mustUnderstand;
    }

    /**
     * Sets the value of the mustUnderstand property.
     *
     * @param value allowed object is {@link String }
     */
    public void setMustUnderstand(Boolean value) {
        this.mustUnderstand = value;
    }

    /**
     * Gets the value of the actor property.
     *
     * @return possible object is {@link String }
     */
    public String getActor() {
        return actor;
    }

    /**
     * Sets the value of the actor property.
     *
     * @param value allowed object is {@link String }
     */
    public void setActor(String value) {
        this.actor = value;
    }

}
