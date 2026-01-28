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
 * Java class for AttributeStatementType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="AttributeStatementType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}StatementAbstractType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedAttribute"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AttributeStatementType extends StatementAbstractType {

    protected List<ASTChoiceType> attributes = new ArrayList<>();

    /**
     * Add an attribute
     *
     * @param attribute
     */
    public void addAttribute(ASTChoiceType attribute) {
        attributes.add(attribute);
    }

    /**
     * Remove an attribute
     *
     * @param attribute
     */
    public void removeAttribute(ASTChoiceType attribute) {
        attributes.remove(attribute);
    }

    /**
     * Gets the attributes.
     *
     * @return a read only {@link List}
     */
    public List<ASTChoiceType> getAttributes() {
        return Collections.unmodifiableList(this.attributes);
    }

    public void addAttributes(List<ASTChoiceType> attributes) {
        this.attributes.addAll(attributes);
    }

    public static class ASTChoiceType implements Serializable {

        private AttributeType attribute;
        private EncryptedElementType encryptedAssertion;

        public ASTChoiceType(AttributeType attribute) {
            super();
            this.attribute = attribute;
        }

        public ASTChoiceType(EncryptedElementType encryptedAssertion) {
            super();
            this.encryptedAssertion = encryptedAssertion;
        }

        public AttributeType getAttribute() {
            return attribute;
        }

        public EncryptedElementType getEncryptedAssertion() {
            return encryptedAssertion;
        }
    }
}