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

package org.keycloak.protocol.saml.installation;

import org.keycloak.saml.common.util.DocumentUtil;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class ModAuthMellonClientInstallationTest {

    private static final String UNSIGNED_DESCRIPTOR = "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"http://localhost/realms/test\">"
            + "<md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
            + "<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</md:NameIDFormat>"
            + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"http://localhost/realms/test/protocol/saml\"/>"
            + "</md:IDPSSODescriptor></md:EntityDescriptor>";

    private static final String SIGNED_DESCRIPTOR = "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"http://localhost/realms/test\">"
            + "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo/><ds:SignatureValue>abc</ds:SignatureValue></ds:Signature>"
            + "<md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"/>"
            + "</md:EntityDescriptor>";

    @Test
    public void unsignedDescriptorIsIndented() throws Exception {
        String formatted = ModAuthMellonClientInstallation.prettyPrintXml(UNSIGNED_DESCRIPTOR);

        assertThat(formatted.lines().count(), greaterThan(1L));
        assertThat(formatted, containsString("Location=\"http://localhost/realms/test/protocol/saml\""));

        Document original = DocumentUtil.getDocument(UNSIGNED_DESCRIPTOR);
        Document reparsed = DocumentUtil.getDocument(formatted);
        Element root = reparsed.getDocumentElement();
        assertThat(root.getLocalName(), is("EntityDescriptor"));
        assertThat(root.getAttribute("entityID"), is("http://localhost/realms/test"));
        assertThat(reparsed.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:metadata", "NameIDFormat").item(0).getTextContent(),
                is(original.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:metadata", "NameIDFormat").item(0).getTextContent()));
    }

    @Test
    public void signedDescriptorIsLeftUntouched() {
        assertThat(ModAuthMellonClientInstallation.prettyPrintXml(SIGNED_DESCRIPTOR), is(SIGNED_DESCRIPTOR));
    }

    @Test
    public void invalidOrEmptyInputIsReturnedAsIs() {
        assertThat(ModAuthMellonClientInstallation.prettyPrintXml("<md:EntityDescriptor"), is("<md:EntityDescriptor"));
        assertThat(ModAuthMellonClientInstallation.prettyPrintXml(""), is(""));
        assertThat(ModAuthMellonClientInstallation.prettyPrintXml(null), nullValue());
    }
}
