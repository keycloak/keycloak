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

/**
 * <p>
 * Java class for AuthnContextDeclarationBaseType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AuthnContextDeclarationBaseType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Identification"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}TechnicalProtection"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}OperationalProtection"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}AuthnMethod" minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}GoverningAgreements"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Extension"
 * maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="ID" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class OriginalAuthnContextDeclarationBaseType extends ExtensionListType {

    protected IdentificationType identification;
    protected TechnicalProtectionBaseType technicalProtection;
    protected OperationalProtectionType operationalProtection;
    protected AuthnMethodBaseType authnMethod;
    protected GoverningAgreementsType governingAgreements;
    protected String id;

    /**
     * Gets the value of the identification property.
     *
     * @return possible object is {@link IdentificationType }
     */
    public IdentificationType getIdentification() {
        return identification;
    }

    /**
     * Sets the value of the identification property.
     *
     * @param value allowed object is {@link IdentificationType }
     */
    public void setIdentification(IdentificationType value) {
        this.identification = value;
    }

    /**
     * Gets the value of the technicalProtection property.
     *
     * @return possible object is {@link TechnicalProtectionBaseType }
     */
    public TechnicalProtectionBaseType getTechnicalProtection() {
        return technicalProtection;
    }

    /**
     * Sets the value of the technicalProtection property.
     *
     * @param value allowed object is {@link TechnicalProtectionBaseType }
     */
    public void setTechnicalProtection(TechnicalProtectionBaseType value) {
        this.technicalProtection = value;
    }

    /**
     * Gets the value of the operationalProtection property.
     *
     * @return possible object is {@link OperationalProtectionType }
     */
    public OperationalProtectionType getOperationalProtection() {
        return operationalProtection;
    }

    /**
     * Sets the value of the operationalProtection property.
     *
     * @param value allowed object is {@link OperationalProtectionType }
     */
    public void setOperationalProtection(OperationalProtectionType value) {
        this.operationalProtection = value;
    }

    /**
     * Gets the value of the authnMethod property.
     *
     * @return possible object is {@link AuthnMethodBaseType }
     */
    public AuthnMethodBaseType getAuthnMethod() {
        return authnMethod;
    }

    /**
     * Sets the value of the authnMethod property.
     *
     * @param value allowed object is {@link AuthnMethodBaseType }
     */
    public void setAuthnMethod(AuthnMethodBaseType value) {
        this.authnMethod = value;
    }

    /**
     * Gets the value of the governingAgreements property.
     *
     * @return possible object is {@link GoverningAgreementsType }
     */
    public GoverningAgreementsType getGoverningAgreements() {
        return governingAgreements;
    }

    /**
     * Sets the value of the governingAgreements property.
     *
     * @param value allowed object is {@link GoverningAgreementsType }
     */
    public void setGoverningAgreements(GoverningAgreementsType value) {
        this.governingAgreements = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is {@link String }
     */
    public String getID() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is {@link String }
     */
    public void setID(String value) {
        this.id = value;
    }

}