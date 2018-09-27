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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;

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
            UriBuilder builder = UriBuilder.fromUri(ServletTestUtils.getAuthServerUrlBase() + "/auth/realms/demo/protocol/saml?SAMLRequest=" + getSamlRequest());
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
    *
    * It should be replaced by dynamically generated code. See KEYCLOAK-8245
    */
    private String getSamlRequest() {
        if (System.getProperty("auth.server.ssl.required", "false").equals("true")) {
            return "jVJLbxshEL5Xyn9A3Ndg%2FNgN8lpyYkW1lDYr2%2B2hl4qw4xiJhQ3Dus2%2FD17HSqqoaQUHBN%2FM9xhmqBrbykUX924Njx1gJL8b61D2DyXtgpNeoUHpVAMoo5abxZdbKQZctsFHr72lp5KPwQoRQjTeUbI4H6%2B9w66BsIFwMBq%2BrW9Luo%2BxlYxZr5Xde4yyEAVn0LTWPwEwSpZJo3HqWH9C45%2FwyXjEVPLDAijbIKuh8ewslR1tUXLjg4bedEl3yiJQslqWdLX8Oa4LmGgQWS74NBvn4j5TxWWeiboY5fySa84nCYxV8mMO8FqO2MHKYVQullTw4STjo4xPt0LItIf5IAn7QUn1IuTKuNq4h48zuz%2BBUH7ebqusuttsKfkOAXvrCUDnZHY0JHv28GZy%2FzuL%2BT%2FinrE3%2FV%2FYWvk1dVwtK2%2BNfiILa%2F2v6xR2TGnE0EEfb6Pi3zUMB8P%2BxtTZrofKzmEL2uwM1JQlHvb%2BX84vPh3XMw%3D%3D";
        }

        return "jZJRT9swFIX%2FiuX31I5pSbCaSoVqWiXYIlp42Asyzu1qybGDr1PWfz83LQKJAZP8YNnf9T3nXE9RtbaT8z5u3S089YCR%2FGmtQzlcVLQPTnqFBqVTLaCMWq7mN9dSjLjsgo9ee0uPJZ%2FDChFCNN5RMn%2FZXnmHfQthBWFnNNzdXld0G2MnGbNeK7v1GGUpSs6g7azfAzBKFkmjcepQ%2Fy86T7RKdlgAZVtkDbSevShlB1eUfPNBw%2BC5ohtlEShZLiq6XDyMmxImGkRWCH6ejQvxmKnyoshEU54V%2FIJrzicJxjrZMTt4LUfsYekwKhcrKng%2ByfhZxs%2FXQsi08mJUTsa%2FKKlPQi6Na4z7%2FXlkj0cI5ff1us7qn6s1JfcQcHCeADoj04MhOXQPbwb3v6OYfZH2lL15%2F9Stkz%2FSi8tF7a3RezK31j9fpbBjSiOGHoZ4WxU%2F1pCP8uHENNlmQGXvsANtNgYaylIf9v5bzv4C";
    }
}
