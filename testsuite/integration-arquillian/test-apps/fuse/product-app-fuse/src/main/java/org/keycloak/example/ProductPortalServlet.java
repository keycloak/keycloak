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

package org.keycloak.example;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.example.ws.Product;
import org.keycloak.example.ws.UnknownProductFault;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet for receiving informations about products from backend JAXWS service
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProductPortalServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        // Send jaxws request
        try (PrintWriter out = resp.getWriter()) {
            out.println("<html><head><title>Product Portal Page</title></head><body>");

            String logoutUri = KeycloakUriBuilder.fromUri("http://localhost:8080/auth")
                    .path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH)
                    .queryParam("redirect_uri", "http://localhost:8181/product-portal")
                    .build("demo")
                    .toString();

            String acctUri = KeycloakUriBuilder.fromUri("http://localhost:8080/auth")
                    .path(ServiceUrlConstants.ACCOUNT_SERVICE_PATH)
                    .queryParam("referrer", "product-portal")
                    .build("demo")
                    .toString();

            out.println("<p>Goto: <a href=\"/customer-portal\">customers</a> | <a href=\"" + logoutUri + "\">logout</a> | <a href=\"" + acctUri + "\">manage acct</a></p>");
            out.println("Servlet User Principal <b>" + req.getUserPrincipal() + "</b> made this request.");

            String unsecuredWsClientResponse = sendWsReq(req, "1", false);
            String securedWsClientResponse = sendWsReq(req, "1", true);
            String securedWsClient2Response = sendWsReq(req, "2", true);

            out.println("<p>Product with ID 1 - unsecured request (it should end with failure): <b>" + unsecuredWsClientResponse + "</b></p><br>");
            out.println("<p>Product with ID 1 - secured request: <b>" + securedWsClientResponse + "</b></p><br>");
            out.println("<p>Product with ID 2 - secured request: <b>" + securedWsClient2Response + "</b></p><br>");
            out.println("</body></html>");
            out.flush();
        }
    }

    private String sendWsReq(HttpServletRequest req, String productId, boolean secured) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Product.class);
        factory.setAddress("http://localhost:8282/ProductServiceCF");

        Product simpleClient = (Product) factory.create();
        Holder<String> _getProduct_productId = new Holder<>(productId);
        Holder<String> _getProduct_name = new Holder<>();

        // Attach Authorization header
        if (secured) {
            Client clientProxy = ClientProxy.getClient(simpleClient);

            KeycloakSecurityContext session = (KeycloakSecurityContext) req.getAttribute(KeycloakSecurityContext.class.getName());
            if (session == null) throw new RuntimeException("Keycloak Security Context is null.");

            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Authorization", Collections.singletonList("Bearer " + session.getTokenString()));

            clientProxy.getRequestContext().put(Message.PROTOCOL_HEADERS, headers);
        }

        try {
            simpleClient.getProduct(_getProduct_productId, _getProduct_name);
            return String.format("Product received: id=%s, name=%s", _getProduct_productId.value, _getProduct_name.value);
        } catch (UnknownProductFault upf) {
            return "UnknownProductFault has occurred. Details: " + upf.toString();
        } catch (WebServiceException wse) {
            String error = "Can't receive product. Reason: " + wse.getMessage();
            if (wse.getCause() != null) {
                Throwable cause = wse.getCause();
                error = error + " Details: " + cause.getClass().getName() + ": " + cause.getMessage();
            }
            return error;
        }
    }
}
