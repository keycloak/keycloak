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

package org.keycloak.testsuite.adapter.servlet;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.UUID;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class SamlSPFacade extends HttpServlet {
    public static String samlResponse;
    public static String RELAY_STATE = "http://test.com/foo/bar";
    public static String sentRelayState;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handler(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handler(req, resp);
    }

    private void handler(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("In SamlSPFacade Servlet handler()");
        if (req.getParameterMap().isEmpty()) {
            System.out.println("ParameterMap is empty, redirecting to keycloak server ");
            resp.setStatus(302);
            // Redirect
            UriBuilder builder = UriBuilder.fromUri(getSamlAuthnRequest(req));
            builder.queryParam("RelayState", RELAY_STATE);
            resp.setHeader("Location", builder.build().toString());
            return;
        }

        System.out.println("Response was received");
        samlResponse = req.getParameter("SAMLResponse");
        sentRelayState = req.getParameter("RelayState");

        PrintWriter pw = resp.getWriter();
        pw.println("Relay state: " + sentRelayState);
        pw.println("SAML response: " + samlResponse);
        pw.flush();
    }

   /*
    * https://idp.ssocircle.com/sso/toolbox/samlEncode.jsp
    *
    * returns (https instead of http in case ssl is required)
    * 
    * <samlp:AuthnRequest 
    *     xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" 
    *     xmlns="urn:oasis:names:tc:SAML:2.0:assertion" 
    *     AssertionConsumerServiceURL="http://localhost:8280/employee/" 
    *     Destination="http://localhost:8180/auth/realms/demo/protocol/saml" 
    *     ForceAuthn="false" 
    *     ID="ID_4d8e5ce2-7206-472b-a897-2d837090c005" 
    *     IsPassive="false" 
    *     IssueInstant="2015-03-06T22:22:17.854Z" 
    *     ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST" 
    *     Version="2.0"> 
    *         <saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">saml-employee</saml:Issuer> 
    *         <samlp:NameIDPolicy AllowCreate="true" Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"/> 
    * </samlp:AuthnRequest> 
    */
    private URI getSamlAuthnRequest(HttpServletRequest req) {
        try {
            BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();
            SAML2Request samlReq = new SAML2Request();
            String appServerUrl = ServletTestUtils.getUrlBase(req) + "/employee/";
            String authServerUrl = ServletTestUtils.getAuthServerUrlBase() + "/auth/realms/demo/protocol/saml";
            AuthnRequestType loginReq;
            loginReq = samlReq.createAuthnRequestType(UUID.randomUUID().toString(), appServerUrl, authServerUrl, "http://localhost:8280/employee/");
            loginReq.getNameIDPolicy().setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.getUri());

            return binding.redirectBinding(SAML2Request.convert(loginReq)).requestURI(authServerUrl);
        } catch (IOException | ConfigurationException | ParsingException | ProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}
