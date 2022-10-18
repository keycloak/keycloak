/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.saml.processing.core.saml.v2.util;

import org.keycloak.dom.saml.v2.assertion.EncryptedElementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.SubjectQueryAbstractType;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.parsers.util.SAMLParserUtil;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.PrivateKey;
import java.util.Collections;

/**
 * @author Ben Cresitello-Dittmar
 *
 * February 2023 this class was created to assist in parsing SAML subjects
 */
public class SubjectUtil {
    /**
     * Determine if the subject includes an encrypted NAMEID
     * @param subjectQueryType The SAML subject
     * @return true if the subject includes an encrypted NAMEID element
     */
    public static boolean isSubjectEncrypted(SubjectQueryAbstractType subjectQueryType){
        EncryptedElementType encryptedNameId = subjectQueryType.getSubject().getSubType().getEncryptedID();
        return encryptedNameId != null;
    }

    /**
     * Decrypt the encrypted NAMEID element in the provided SAML subject with the given private key.
     * @param subjectQueryType The SAML subject including the encrypted NAMEID element
     * @param privateKey The private key to decrypt the NAMEID
     * @return The decrypted NAMEID
     * @throws ProcessingException
     * @throws ParsingException
     * @throws ConfigurationException
     */
    public static Element decryptSubject(SubjectQueryAbstractType subjectQueryType, PrivateKey privateKey) throws ProcessingException, ParsingException, ConfigurationException  {
        Element enc = subjectQueryType.getSubject().getSubType().getEncryptedID().getEncryptedElement();

        if (enc == null) {
            throw new ProcessingException("No encrypted subject found.");
        }

        // create new doc with only encrypted subject
        Document newDoc = org.keycloak.saml.common.util.DocumentUtil.createDocument();
        Node importedNode = newDoc.importNode(enc, true);
        newDoc.appendChild(importedNode);

        // decrypt element
        Element decryptedDocumentElement = XMLEncryptionUtil.decryptElementInDocument(newDoc, data -> Collections.singletonList(privateKey));

        JAXPValidationUtil.checkSchemaValidation(decryptedDocumentElement);

        // update document
        NameIDType newNameId = SAMLParserUtil.parseNameIDType((SAMLParser.createEventReader(DocumentUtil.getNodeAsStream(decryptedDocumentElement))));
        subjectQueryType.getSubject().getSubType().addBaseID(newNameId);

        return enc;
    }
}
