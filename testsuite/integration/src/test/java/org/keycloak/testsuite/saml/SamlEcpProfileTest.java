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

package org.keycloak.testsuite.saml;

import org.jboss.resteasy.util.Base64;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusCodeType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testsuite.samlfilter.SamlAdapterTest;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlEcpProfileTest {

    protected String APP_SERVER_BASE_URL = "http://localhost:8081";

    @ClassRule
    public static org.keycloak.testsuite.samlfilter.SamlKeycloakRule keycloakRule = new org.keycloak.testsuite.samlfilter.SamlKeycloakRule() {
        @Override
        public void initWars() {
            ClassLoader classLoader = SamlAdapterTest.class.getClassLoader();

            initializeSamlSecuredWar("/keycloak-saml/ecp/ecp-sp", "/ecp-sp",  "ecp-sp.war", classLoader);
        }

        @Override
        public String getRealmJson() {
            return "/keycloak-saml/ecp/testsamlecp.json";
        }
    };

    @Test
    public void testSuccessfulEcpFlow() throws Exception {
        Response authnRequestResponse = ClientBuilder.newClient().target(APP_SERVER_BASE_URL + "/ecp-sp/").request()
                .header("Accept", "text/html; application/vnd.paos+xml")
                .header("PAOS", "ver='urn:liberty:paos:2003-08' ;'urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp'")
                .get();

        SOAPMessage authnRequestMessage = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(authnRequestResponse.readEntity(byte[].class)));

        printDocument(authnRequestMessage.getSOAPPart().getContent(), System.out);

        Iterator<SOAPHeaderElement> it = authnRequestMessage.getSOAPHeader().<SOAPHeaderElement>getChildElements(new QName("urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp", "Request"));
        SOAPHeaderElement ecpRequestHeader = it.next();
        NodeList idpList = ecpRequestHeader.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "IDPList");

        assertEquals("No IDPList returned from Service Provider", 1, idpList.getLength());

        NodeList idpEntries = idpList.item(0).getChildNodes();

        assertEquals("No IDPEntry returned from Service Provider", 1, idpEntries.getLength());

        String singleSignOnService = null;

        for (int i = 0; i < idpEntries.getLength(); i++) {
            Node item = idpEntries.item(i);
            NamedNodeMap attributes = item.getAttributes();
            Node location = attributes.getNamedItem("Loc");

            singleSignOnService = location.getNodeValue();
        }

        assertNotNull("Could not obtain SSO Service URL", singleSignOnService);

        Document authenticationRequest = authnRequestMessage.getSOAPBody().getFirstChild().getOwnerDocument();
        String username = "pedroigor";
        String password = "password";
        String pair = username + ":" + password;
        String authHeader = "Basic " + new String(Base64.encodeBytes(pair.getBytes()));

        Response authenticationResponse = ClientBuilder.newClient().target(singleSignOnService).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(Entity.entity(DocumentUtil.asString(authenticationRequest), "text/xml"));

        assertEquals(OK.getStatusCode(), authenticationResponse.getStatus());

        SOAPMessage responseMessage  = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(authenticationResponse.readEntity(byte[].class)));

        printDocument(responseMessage.getSOAPPart().getContent(), System.out);

        SOAPHeader responseMessageHeaders = responseMessage.getSOAPHeader();

        NodeList ecpResponse = responseMessageHeaders.getElementsByTagNameNS(JBossSAMLURIConstants.ECP_PROFILE.get(), JBossSAMLConstants.RESPONSE.get());

        assertEquals("No ECP Response", 1, ecpResponse.getLength());

        Node samlResponse = responseMessage.getSOAPBody().getFirstChild();

        assertNotNull(samlResponse);

        ResponseType responseType = (ResponseType) new SAMLParser().parse(samlResponse);
        StatusCodeType statusCode = responseType.getStatus().getStatusCode();

        assertEquals(statusCode.getValue().toString(), JBossSAMLURIConstants.STATUS_SUCCESS.get());
        assertEquals("http://localhost:8081/ecp-sp/", responseType.getDestination());
        assertNotNull(responseType.getSignature());
        assertEquals(1, responseType.getAssertions().size());

        SOAPMessage samlResponseRequest = MessageFactory.newInstance().createMessage();

        samlResponseRequest.getSOAPBody().addDocument(responseMessage.getSOAPBody().extractContentAsDocument());

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        samlResponseRequest.writeTo(os);

        Response serviceProviderFinalResponse = ClientBuilder.newClient().target(responseType.getDestination()).request()
                .post(Entity.entity(os.toByteArray(), "application/vnd.paos+xml"));

        Map<String, NewCookie> cookies = serviceProviderFinalResponse.getCookies();

        Builder resourceRequest = ClientBuilder.newClient().target(responseType.getDestination() + "/index.html").request();

        for (NewCookie cookie : cookies.values()) {
            resourceRequest.cookie(cookie);
        }

        Response resourceResponse = resourceRequest.get();

        assertTrue(resourceResponse.readEntity(String.class).contains("pedroigor"));
    }

    @Test
    public void testInvalidCredentials() throws Exception {
        Response authnRequestResponse = ClientBuilder.newClient().target(APP_SERVER_BASE_URL + "/ecp-sp/").request()
                .header("Accept", "text/html; application/vnd.paos+xml")
                .header("PAOS", "ver='urn:liberty:paos:2003-08' ;'urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp'")
                .get();

        SOAPMessage authnRequestMessage = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(authnRequestResponse.readEntity(byte[].class)));
        Iterator<SOAPHeaderElement> it = authnRequestMessage.getSOAPHeader().<SOAPHeaderElement>getChildElements(new QName("urn:liberty:paos:2003-08", "Request"));

        it.next();

        it = authnRequestMessage.getSOAPHeader().<SOAPHeaderElement>getChildElements(new QName("urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp", "Request"));
        SOAPHeaderElement ecpRequestHeader = it.next();
        NodeList idpList = ecpRequestHeader.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:protocol", "IDPList");

        assertEquals("No IDPList returned from Service Provider", 1, idpList.getLength());

        NodeList idpEntries = idpList.item(0).getChildNodes();

        assertEquals("No IDPEntry returned from Service Provider", 1, idpEntries.getLength());

        String singleSignOnService = null;

        for (int i = 0; i < idpEntries.getLength(); i++) {
            Node item = idpEntries.item(i);
            NamedNodeMap attributes = item.getAttributes();
            Node location = attributes.getNamedItem("Loc");

            singleSignOnService = location.getNodeValue();
        }

        assertNotNull("Could not obtain SSO Service URL", singleSignOnService);

        Document authenticationRequest = authnRequestMessage.getSOAPBody().getFirstChild().getOwnerDocument();
        String username = "pedroigor";
        String password = "baspassword";
        String pair = username + ":" + password;
        String authHeader = "Basic " + new String(Base64.encodeBytes(pair.getBytes()));

        Response authenticationResponse = ClientBuilder.newClient().target(singleSignOnService).request()
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .post(Entity.entity(DocumentUtil.asString(authenticationRequest), "application/soap+xml"));

        assertEquals(OK.getStatusCode(), authenticationResponse.getStatus());

        SOAPMessage responseMessage  = MessageFactory.newInstance().createMessage(null, new ByteArrayInputStream(authenticationResponse.readEntity(byte[].class)));
        Node samlResponse = responseMessage.getSOAPBody().getFirstChild();

        assertNotNull(samlResponse);

        StatusResponseType responseType = (StatusResponseType) new SAMLParser().parse(samlResponse);
        StatusCodeType statusCode = responseType.getStatus().getStatusCode();

        assertNotEquals(statusCode.getStatusCode().getValue().toString(), JBossSAMLURIConstants.STATUS_SUCCESS.get());
    }

    public static void printDocument(Source doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(doc,
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
}
