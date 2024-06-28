/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.saml.processing.core.parsers.saml.xmldsig;

import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import javax.xml.namespace.QName;
import org.keycloak.saml.processing.core.parsers.util.HasQName;

/**
 * Elements from saml-schema-protocol-2.0.xsd
 * @author hmlnarik
 */
public enum XmlDSigQNames implements HasQName {

    CANONICALIZATION_METHOD("CanonicalizationMethod"),
    DIGEST_METHOD("DigestMethod"),
    DIGEST_VALUE("DigestValue"),
    DSA_KEY_VALUE("DSAKeyValue"),
    EXPONENT("Exponent"),
    G("G"),
    HMAC_OUTPUT_LENGTH("HMACOutputLength"),
    J("J"),
    KEY_INFO("KeyInfo"),
    KEY_NAME("KeyName"),
    KEY_VALUE("KeyValue"),
    MANIFEST("Manifest"),
    MGMT_DATA("MgmtData"),
    MODULUS("Modulus"),
    OBJECT("Object"),
    PGEN_COUNTER("PgenCounter"),
    PGP_DATA("PGPData"),
    PGP_KEY_ID("PGPKeyID"),
    PGP_KEY_PACKET("PGPKeyPacket"),
    P("P"),
    Q("Q"),
    REFERENCE("Reference"),
    RETRIEVAL_METHOD("RetrievalMethod"),
    RSA_KEY_VALUE("RSAKeyValue"),
    SEED("Seed"),
    SIGNATURE_METHOD("SignatureMethod"),
    SIGNATURE_PROPERTIES("SignatureProperties"),
    SIGNATURE_PROPERTY("SignatureProperty"),
    SIGNATURE("Signature"),
    SIGNATURE_VALUE("SignatureValue"),
    SIGNED_INFO("SignedInfo"),
    SPKI_DATA("SPKIData"),
    SPKIS_EXP("SPKISexp"),
    TRANSFORMS("Transforms"),
    TRANSFORM("Transform"),
    XPATH("XPath"),
    X509_CERTIFICATE("X509Certificate"),
    X509_CRL("X509CRL"),
    X509_DATA("X509Data"),
    X509_ISSUER_NAME("X509IssuerName"),
    X509_ISSUER_SERIAL("X509IssuerSerial"),
    X509_SERIAL_NUMBER("X509SerialNumber"),
    X509_SKI("X509SKI"),
    X509_SUBJECT_NAME("X509SubjectName"),
    Y("Y"),

    UNKNOWN_ELEMENT("")
    ;

    private final QName qName;

    XmlDSigQNames(String localName) {
        this(JBossSAMLURIConstants.XMLDSIG_NSURI, localName);
    }

    XmlDSigQNames(HasQName source) {
        this.qName = source.getQName();
    }

    XmlDSigQNames(JBossSAMLURIConstants nsUri, String localName) {
        this.qName = new QName(nsUri == null ? null : nsUri.get(), localName);
    }

    @Override
    public QName getQName() {
        return qName;
    }
}
