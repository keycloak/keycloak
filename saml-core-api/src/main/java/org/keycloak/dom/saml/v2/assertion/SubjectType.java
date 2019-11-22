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
package org.keycloak.dom.saml.v2.assertion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Java class for SubjectType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SubjectType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;sequence>
 *           &lt;choice>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}BaseID"/>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}NameID"/>
 *             &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedID"/>
 *           &lt;/choice>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}SubjectConfirmation" maxOccurs="unbounded"
 * minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}SubjectConfirmation" maxOccurs="unbounded"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class SubjectType implements Serializable {

    protected List<SubjectConfirmationType> subjectConfirmation = new ArrayList<>();

    protected STSubType subType;

    /**
     * Get the {@link STSubType}
     *
     * @return
     */
    public STSubType getSubType() {
        return subType;
    }

    /**
     * Set the {@link STSubType}
     *
     * @param subType
     */
    public void setSubType(STSubType subType) {
        this.subType = subType;
    }

    /**
     * Get the size of subject confirmations
     *
     * @return
     */
    public int getCount() {
        return subjectConfirmation.size();
    }

    /**
     * Get a list of subject confirmations
     *
     * @return {@link} read only list of subject confirmation
     */
    public List<SubjectConfirmationType> getConfirmation() {
        return Collections.unmodifiableList(subjectConfirmation);
    }

    /**
     * Add a subject confirmation
     *
     * @param con
     */
    public void addConfirmation(SubjectConfirmationType con) {
        subjectConfirmation.add(con);
    }

    /**
     * Remove a subject confirmation
     *
     * @param con
     */
    public void removeConfirmation(SubjectConfirmationType con) {
        subjectConfirmation.remove(con);
    }

    public static class STSubType implements Serializable {

        private BaseIDAbstractType baseID;

        private EncryptedElementType encryptedID;

        protected List<SubjectConfirmationType> subjectConfirmation = new ArrayList<SubjectConfirmationType>();

        public void addBaseID(BaseIDAbstractType base) {
            this.baseID = base;
        }

        public BaseIDAbstractType getBaseID() {
            return baseID;
        }

        public EncryptedElementType getEncryptedID() {
            return encryptedID;
        }

        public void setEncryptedID(EncryptedElementType encryptedID) {
            this.encryptedID = encryptedID;
        }

        public void addConfirmation(SubjectConfirmationType con) {
            subjectConfirmation.add(con);
        }

        public int getCount() {
            return subjectConfirmation.size();
        }

        public List<SubjectConfirmationType> getConfirmation() {
            return Collections.unmodifiableList(subjectConfirmation);
        }
    }
}