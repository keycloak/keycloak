/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.saml;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A mutable view over a POST-binding SAML response that a test, acting as a service provider, receives from
 * an identity provider. It decodes the base64 form value into a DOM document, exposes helpers to inspect and
 * tamper with the assertion, and re-encodes it for posting back.
 * <p>
 * The realms used by SAML broker tests are typically unsigned, so mutating the decoded document does not
 * invalidate any signature.
 */
public class SamlResponseDocument {

    private static final String ASSERTION_NS = JBossSAMLURIConstants.ASSERTION_NSURI.get();

    public static final String BEARER_METHOD = JBossSAMLURIConstants.SUBJECT_CONFIRMATION_BEARER.get();

    public static final String HOLDER_OF_KEY_METHOD = JBossSAMLURIConstants.HOLDER_OF_KEY.get();

    private final org.w3c.dom.Document document;

    private SamlResponseDocument(org.w3c.dom.Document document) {
        this.document = document;
    }

    /**
     * Parses a base64 POST-binding {@code SAMLResponse} form value into a mutable document.
     */
    public static SamlResponseDocument fromPostBinding(String base64SamlResponse) {
        byte[] xml = Base64.getMimeDecoder().decode(base64SamlResponse);
        try {
            return new SamlResponseDocument(DocumentUtil.getDocument(new String(xml, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse SAML response document", e);
        }
    }

    public org.w3c.dom.Document getDocument() {
        return document;
    }

    /**
     * Returns an independent deep copy of this response, so it can be mutated without affecting the original.
     */
    public SamlResponseDocument copy() {
        return new SamlResponseDocument((org.w3c.dom.Document) document.cloneNode(true));
    }

    /**
     * Replaces the value of the first assertion's subject {@code <saml:NameID>}, i.e. the principal the response
     * claims to authenticate.
     */
    public SamlResponseDocument setNameId(String value) {
        Element nameId = (Element) getFirstAssertionSubject().getElementsByTagNameNS(ASSERTION_NS, "NameID").item(0);
        if (nameId == null) {
            throw new IllegalStateException("SAML assertion subject does not contain a NameID");
        }
        nameId.setTextContent(value);
        return this;
    }

    /**
     * Returns the {@code <saml:Conditions>} element of the first assertion, creating an empty one if the
     * assertion has none.
     */
    public Element getFirstAssertionConditions() {
        Element conditions = (Element) document.getElementsByTagNameNS(ASSERTION_NS, "Conditions").item(0);
        if (conditions == null) {
            Element assertion = (Element) document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
            if (assertion == null) {
                throw new IllegalStateException("SAML response does not contain an assertion");
            }
            conditions = createAssertionElement("Conditions");
            assertion.appendChild(conditions);
        }
        return conditions;
    }

    /**
     * Creates (but does not attach) an element in the SAML assertion namespace, e.g. {@code "ProxyRestriction"}.
     */
    public Element createAssertionElement(String localName) {
        return document.createElementNS(ASSERTION_NS, "saml:" + localName);
    }

    /**
     * Turns the response into an unsolicited (IdP-initiated) one addressed to {@code destination}: the response
     * {@code Destination} and every {@code SubjectConfirmationData Recipient} are set to it, and all
     * {@code InResponseTo} attributes are removed.
     */
    public SamlResponseDocument toUnsolicited(String destination) {
        Element response = document.getDocumentElement();
        response.setAttribute("Destination", destination);
        response.removeAttribute("InResponseTo");
        NodeList data = document.getElementsByTagNameNS(ASSERTION_NS, "SubjectConfirmationData");
        for (int i = 0; i < data.getLength(); i++) {
            Element element = (Element) data.item(i);
            element.setAttribute("Recipient", destination);
            element.removeAttribute("InResponseTo");
        }
        return this;
    }

    private Element getFirstAssertionSubject() {
        Element assertion = (Element) document.getElementsByTagNameNS(ASSERTION_NS, "Assertion").item(0);
        if (assertion == null) {
            throw new IllegalStateException("SAML response does not contain an assertion");
        }
        Element subject = (Element) document.getElementsByTagNameNS(ASSERTION_NS, "Subject").item(0);
        if (subject == null) {
            throw new IllegalStateException("SAML assertion does not contain a subject");
        }
        return subject;
    }

    private List<Element> getSubjectConfirmations() {
        NodeList nodes = getFirstAssertionSubject().getElementsByTagNameNS(ASSERTION_NS, "SubjectConfirmation");
        List<Element> confirmations = new ArrayList<>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            confirmations.add((Element) nodes.item(i));
        }
        return confirmations;
    }

    /**
     * Removes every {@code <saml:SubjectConfirmation>} from the assertion subject.
     */
    public SamlResponseDocument removeSubjectConfirmations() {
        for (Element confirmation : getSubjectConfirmations()) {
            confirmation.getParentNode().removeChild(confirmation);
        }
        return this;
    }

    /**
     * Appends a {@code <saml:SubjectConfirmation>} with the given {@code Method} and no
     * {@code <saml:SubjectConfirmationData>}.
     */
    public SamlResponseDocument addSubjectConfirmation(String method) {
        Element confirmation = createAssertionElement("SubjectConfirmation");
        confirmation.setAttribute("Method", method);
        getFirstAssertionSubject().appendChild(confirmation);
        return this;
    }

    /**
     * Expires the {@code <saml:SubjectConfirmationData>} for the specified confirmation method.
     */
    public SamlResponseDocument expireSubjectConfirmationData(String method) {
        Element data = getSubjectConfirmations().stream()
                .filter(confirmation -> method.equals(confirmation.getAttribute("Method")))
                .map(confirmation -> (Element) confirmation.getElementsByTagNameNS(ASSERTION_NS, "SubjectConfirmationData").item(0))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No SubjectConfirmationData found for method " + method));
        data.setAttribute("NotOnOrAfter", "2000-01-01T00:00:00Z");
        return this;
    }

    /**
     * Appends a {@code <saml:ProxyRestriction Count="count">} condition to the first assertion. A {@code Count}
     * of {@code 0} forbids a proxying party (e.g. an identity broker) from issuing a further assertion.
     */
    public SamlResponseDocument addProxyRestriction(int count) {
        Element proxyRestriction = createAssertionElement("ProxyRestriction");
        proxyRestriction.setAttribute("Count", Integer.toString(count));
        getFirstAssertionConditions().appendChild(proxyRestriction);
        return this;
    }

    /**
     * Signs the response document in place with an enveloped XML signature covering the whole message
     * (including the assertion), using the given key material. This must be called after any mutation (e.g.
     * {@link #addProxyRestriction(int)}) so the signature covers the final content, reproducing a validly
     * signed response as a real identity provider would issue.
     */
    public SamlResponseDocument sign(KeyPair keyPair, String keyName, X509Certificate certificate) {
        try {
            new BaseSAML2BindingBuilder()
                    .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                    .signWith(keyName, keyPair, certificate)
                    .signDocument(document);
            return this;
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign SAML response", e);
        }
    }

    /**
     * Serialises the (possibly mutated) document back into a base64 POST-binding {@code SAMLResponse} value.
     */
    public String toPostBinding() {
        try {
            String xml = DocumentUtil.getDocumentAsString(document);
            return Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Unable to serialise SAML response document", e);
        }
    }

    /**
     * Serialises the document into an unsigned, deflated and URL-encoded HTTP-Redirect {@code SAMLResponse}
     * value.
     */
    public String toRedirectBinding() {
        try {
            return RedirectBindingUtil.deflateBase64URLEncode(DocumentUtil.getDocumentAsString(document));
        } catch (Exception e) {
            throw new RuntimeException("Unable to serialise SAML response document", e);
        }
    }

    /**
     * Serialises the document into an HTTP-Redirect query string ({@code SAMLResponse}, optional
     * {@code RelayState}, {@code SigAlg} and {@code Signature}) whose query signature is computed with the given
     * key, as an identity provider using the redirect binding would issue it.
     */
    public String toSignedRedirectQuery(KeyPair keyPair, String relayState) {
        try {
            BaseSAML2BindingBuilder builder = new BaseSAML2BindingBuilder()
                    .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                    .signWith(null, keyPair)
                    .signDocument();
            if (relayState != null) {
                builder.relayState(relayState);
            }
            return builder.redirectBinding(document).responseURI("http://localhost/").getRawQuery();
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign SAML response", e);
        }
    }
}
