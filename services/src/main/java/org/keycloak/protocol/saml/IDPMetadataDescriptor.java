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

package org.keycloak.protocol.saml;

import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.keycloak.crypto.KeyWrapper;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.util.SAMLMetadataUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.PROTOCOL_NSURI;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.SAML_HTTP_POST_BINDING;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.SAML_SOAP_BINDING;
import static org.keycloak.saml.common.constants.JBossSAMLURIConstants.XMLDSIG_NSURI;

/**
 * @version $Revision: 1 $
 */
public class IDPMetadataDescriptor {

    public static String getIDPDescriptor(URI loginPostEndpoint, URI loginRedirectEndpoint, URI logoutEndpoint,
            URI artifactResolutionService, String entityId, boolean wantAuthnRequestsSigned, List<Element> signingCerts)
            throws ProcessingException {
        return getIDPDescriptor(null, null, loginPostEndpoint, loginRedirectEndpoint, logoutEndpoint,
                artifactResolutionService, entityId, wantAuthnRequestsSigned, signingCerts, null);
    }

    public static String getIDPDescriptor(KeyWrapper keyWrapper, SignatureAlgorithm sigAlg,
            URI loginPostEndpoint, URI loginRedirectEndpoint, URI logoutEndpoint,
            URI artifactResolutionService, String entityId, boolean wantAuthnRequestsSigned, List<Element> signingCerts)
            throws ProcessingException {
        return getIDPDescriptor(null, null, loginPostEndpoint, loginRedirectEndpoint, logoutEndpoint,
                artifactResolutionService, entityId, wantAuthnRequestsSigned, signingCerts, null);
    }

    public static String getIDPDescriptor(KeyWrapper keyWrapper, SignatureAlgorithm sigAlg,
            URI loginPostEndpoint, URI loginRedirectEndpoint, URI logoutEndpoint,
            URI artifactResolutionService, String entityId, boolean wantAuthnRequestsSigned, List<Element> signingCerts, Long expiration)
            throws ProcessingException {

        EntityDescriptorType entityDescriptor = new EntityDescriptorType(entityId);
        if (expiration != null) {
            try {
                Duration cacheDuration = DatatypeFactory.newInstance().newDuration(TimeUnit.SECONDS.toMillis(expiration));
                entityDescriptor.setCacheDuration(cacheDuration);
            } catch (DatatypeConfigurationException e) {
                throw new ProcessingException("Cannot create datatype factory to create duration", e);
            }
        }

        IDPSSODescriptorType spIDPDescriptor = new IDPSSODescriptorType(Arrays.asList(PROTOCOL_NSURI.get()));
        spIDPDescriptor.setWantAuthnRequestsSigned(wantAuthnRequestsSigned);
        spIDPDescriptor.addNameIDFormat(NAMEID_FORMAT_PERSISTENT.get());
        spIDPDescriptor.addNameIDFormat(NAMEID_FORMAT_TRANSIENT.get());
        spIDPDescriptor.addNameIDFormat(NAMEID_FORMAT_UNSPECIFIED.get());
        spIDPDescriptor.addNameIDFormat(NAMEID_FORMAT_EMAIL.get());

        spIDPDescriptor.addSingleLogoutService(new EndpointType(SAML_HTTP_POST_BINDING.getUri(), logoutEndpoint));
        spIDPDescriptor.addSingleLogoutService(new EndpointType(SAML_HTTP_REDIRECT_BINDING.getUri(), logoutEndpoint));
        spIDPDescriptor.addSingleLogoutService(new EndpointType(SAML_HTTP_ARTIFACT_BINDING.getUri(), logoutEndpoint));
        spIDPDescriptor.addSingleLogoutService(new EndpointType(SAML_SOAP_BINDING.getUri(), logoutEndpoint));
        spIDPDescriptor.addSingleSignOnService(new EndpointType(SAML_HTTP_POST_BINDING.getUri(), loginPostEndpoint));
        spIDPDescriptor.addSingleSignOnService(new EndpointType(SAML_HTTP_REDIRECT_BINDING.getUri(), loginRedirectEndpoint));
        spIDPDescriptor.addSingleSignOnService(new EndpointType(SAML_SOAP_BINDING.getUri(), loginPostEndpoint));
        spIDPDescriptor.addSingleSignOnService(new EndpointType(SAML_HTTP_ARTIFACT_BINDING.getUri(), loginPostEndpoint));

        spIDPDescriptor.addArtifactResolutionService(new IndexedEndpointType(SAML_SOAP_BINDING.getUri(), artifactResolutionService));

        if (wantAuthnRequestsSigned && signingCerts != null) {
            for (Element key: signingCerts)
            {
                KeyDescriptorType keyDescriptor = new KeyDescriptorType();
                keyDescriptor.setUse(KeyTypes.SIGNING);
                keyDescriptor.setKeyInfo(key);
                spIDPDescriptor.addKeyDescriptor(keyDescriptor);
            }
        }

        entityDescriptor.addChoiceType(new EntityDescriptorType.EDTChoiceType(Arrays.asList(new EntityDescriptorType.EDTDescriptorChoiceType(spIDPDescriptor))));

        if (sigAlg != null && keyWrapper != null && keyWrapper.getCertificate() != null && keyWrapper.getPrivateKey() != null) {
            try {
                return SAMLMetadataUtil.signEntityDescriptorType(entityDescriptor, sigAlg, keyWrapper.getKid(),
                        keyWrapper.getCertificate(),
                        new KeyPair(keyWrapper.getCertificate().getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()));
            } catch (ConfigurationException | ParsingException e) {
                throw new ProcessingException("Cannot sign IDP metadata with key " + keyWrapper.getKid(), e);
            }
        } else {
            return SAMLMetadataUtil.writeEntityDescriptorType(entityDescriptor);
        }
    }

    public static Element buildKeyInfoElement(String keyName, String pemEncodedCertificate)
        throws javax.xml.parsers.ParserConfigurationException
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element keyInfo = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:KeyInfo");

        if (keyName != null) {
            Element keyNameElement = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:KeyName");
            keyNameElement.setTextContent(keyName);
            keyInfo.appendChild(keyNameElement);
        }

        Element x509Data = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:X509Data");

        Element x509Certificate = doc.createElementNS(XMLDSIG_NSURI.get(), "ds:X509Certificate");
        x509Certificate.setTextContent(pemEncodedCertificate);
      
        x509Data.appendChild(x509Certificate);

        keyInfo.appendChild(x509Data);

        return keyInfo;
    }
}
