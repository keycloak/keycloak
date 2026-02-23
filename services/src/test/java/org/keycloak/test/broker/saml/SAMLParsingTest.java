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

package org.keycloak.test.broker.saml;

import java.io.IOException;
import java.util.Base64;

import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * This was failing on IBM JDK
 * 
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SAMLParsingTest {

    private static final String SAML_RESPONSE = "<samlp:LogoutResponse xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" Destination=\"http://localhost:8081/auth/realms/realm-with-broker/broker/kc-saml-idp-basic/endpoint\" ID=\"ID_9a171d23-c417-42f5-9bca-c093123fd68c\" InResponseTo=\"ID_bc730711-2037-43f3-ad76-7bc33842fb87\" IssueInstant=\"2016-02-29T12:00:14.044Z\" Version=\"2.0\"><saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">http://localhost:8082/auth/realms/realm-with-saml-idp-basic</saml:Issuer><samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status></samlp:LogoutResponse>";
    private static final String SAML_REQUEST = "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" xmlns=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" AssertionConsumerServiceURL=\"http://localhost:8080/realms/master/broker/saml/endpoint\" AttributeConsumingServiceIndex=\"0\" Destination=\"http://localhost:8080/realms/saml/protocol/saml\" ForceAuthn=\"false\" ID=\"ID_7228aef5-4a58-4481-a371-30e4ad7e98f4\" IssueInstant=\"2026-02-16T11:23:32.472Z\" ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Version=\"2.0\"><saml:Issuer>http://localhost:8080/realms/master</saml:Issuer><samlp:NameIDPolicy AllowCreate=\"true\" Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\"/></samlp:AuthnRequest>";

    @Test
    public void parseTest() {
        String base64 = Base64.getEncoder().encodeToString(SAML_RESPONSE.getBytes(GeneralConstants.SAML_CHARSET));
        byte[] samlBytes = PostBindingUtil.base64Decode(base64);
        SAMLDocumentHolder holder = SAMLRequestParser.parseResponseDocument(samlBytes);
        Assert.assertNotNull(holder);
    }

    @Test
    public void parseMimeTest() {
        String base64 = Base64.getMimeEncoder().encodeToString(SAML_RESPONSE.getBytes(GeneralConstants.SAML_CHARSET));
        byte[] samlBytes = PostBindingUtil.base64Decode(base64);
        SAMLDocumentHolder holder = SAMLRequestParser.parseResponseDocument(samlBytes);
        Assert.assertNotNull(holder);
    }

    @Test
    public void parseRequestResponseRedirectBinding() throws IOException {
        String encodedResponse = RedirectBindingUtil.deflateBase64Encode(SAML_RESPONSE.getBytes(GeneralConstants.SAML_CHARSET_NAME));
        SAMLDocumentHolder holder = SAMLRequestParser.parseResponseRedirectBinding(encodedResponse, SAML_RESPONSE.length());
        Assert.assertNotNull(holder);
        holder = SAMLRequestParser.parseResponseRedirectBinding(encodedResponse, SAML_RESPONSE.length() - 1);
        Assert.assertNull(holder);

        String encodedRequest = RedirectBindingUtil.deflateBase64Encode(SAML_REQUEST.getBytes(GeneralConstants.SAML_CHARSET_NAME));
        holder = SAMLRequestParser.parseRequestRedirectBinding(encodedRequest, SAML_REQUEST.length());
        Assert.assertNotNull(holder);
        holder = SAMLRequestParser.parseRequestRedirectBinding(encodedRequest, SAML_RESPONSE.length() - 1);
        Assert.assertNull(holder);
    }
}
