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
 * Java class for IdentificationType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="IdentificationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}PhysicalVerification"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}WrittenConsent"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}GoverningAgreements"
 * minOccurs="0"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}Extension"
 * maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="nym" type="{urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony}nymType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class IdentificationType extends ExtensionListType {

    protected PhysicalVerification physicalVerification;
    protected ExtensionOnlyType writtenConsent;
    protected GoverningAgreementsType governingAgreements;
    protected NymType nym;

    /**
     * Gets the value of the physicalVerification property.
     *
     * @return possible object is {@link PhysicalVerification }
     */
    public PhysicalVerification getPhysicalVerification() {
        return physicalVerification;
    }

    /**
     * Sets the value of the physicalVerification property.
     *
     * @param value allowed object is {@link PhysicalVerification }
     */
    public void setPhysicalVerification(PhysicalVerification value) {
        this.physicalVerification = value;
    }

    /**
     * Gets the value of the writtenConsent property.
     *
     * @return possible object is {@link ExtensionOnlyType }
     */
    public ExtensionOnlyType getWrittenConsent() {
        return writtenConsent;
    }

    /**
     * Sets the value of the writtenConsent property.
     *
     * @param value allowed object is {@link ExtensionOnlyType }
     */
    public void setWrittenConsent(ExtensionOnlyType value) {
        this.writtenConsent = value;
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
     * Gets the value of the nym property.
     *
     * @return possible object is {@link NymType }
     */
    public NymType getNym() {
        return nym;
    }

    /**
     * Sets the value of the nym property.
     *
     * @param value allowed object is {@link NymType }
     */
    public void setNym(NymType value) {
        this.nym = value;
    }

}
