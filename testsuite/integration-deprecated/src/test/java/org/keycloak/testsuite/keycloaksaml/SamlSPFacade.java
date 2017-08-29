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

package org.keycloak.testsuite.keycloaksaml;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;

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

    private void handler(HttpServletRequest req, HttpServletResponse resp) {
        System.out.println("********* HERE ******");
        if (req.getParameterMap().isEmpty()) {
            System.out.println("redirecting");
            resp.setStatus(302);
            // Redirect
            // UriBuilder builder = UriBuilder.fromUri("http://localhost:8081/auth/realms/demo/protocol/saml?SAMLRequest=jVLRTsIwFP2Vpe%2BjG4wxG0YyWYxL0BBAH3wx3XYnTbp29nYof%2B8YEvEBNOlD03vOveec2ynyWjYsae1WreC9BbTOZy0Vsr4Qk9YopjkKZIrXgMwWbJ08LNhw4LHGaKsLLcmRch3MEcFYoRVxktN1rhW2NZg1mJ0o4Gm1iMnW2oZRKnXB5VajZZEX%2BRTqRuo9ACVO2mkUih%2F4l9C8s0MNcFkjLaHW9KSUHlwR506bAnrPMam4RCBOlsYkS1%2BD3MvLcDJxAx9KN4jCkXszrG5cP%2BCVH4y8IM8PYFx2dsQOfuiILWQKLVc2JkPPH7te6HrRxh%2BzUdidwSSIXoiz%2FBZyK1Qp1Nv1yPIjCNn9ZrN0V1AKA4UlzjMY7N13IDKbHjyxXoA5291%2FtzH7I%2FApPet%2FHNawx65hli61FMXeSaTUH%2FMubtvlYU0LfcA1t5cl%2BAO%2FfxGlW%2FVQ1ipsoBCVgJLQ2XHo7385%2BwI%3D");
            UriBuilder builder = UriBuilder.fromUri("http://localhost:8081/auth/realms/demo/protocol/saml?SAMLRequest=jVJbT8IwFP4rS99HuwluNIwEIUYSLwugD76Y2h2kSdfOng7l31uGRn0ATfrQ9HznfJfTEYpaN3zS%2Bo1ZwGsL6KP3WhvkXaEgrTPcClTIjagBuZd8Obm55mmP8cZZb6XV5NByGiwQwXllDYkmX9epNdjW4JbgtkrC%2FeK6IBvvG06ptlLojUXPc5YnFOpG2x0AJdEsaFRG7PuPoUWwQx0IXSOtoLb0SynduyLRpXUSOs8FWQuNQKL5rCDz2VO%2FymEgIY2zlJ3H%2FSx9jkU%2BzOK0ys8yNmSSsUEAYxnsqC18tyO2MDfohfEFSVkyiNlZzM5XacrDSbJePug%2Fkqj8FHKhTKXMy%2BnIng8g5FerVRmXd8sViR7AYec8AMh4tPfDO3L3Y2%2F%2F3cT4j7BH9Mf8A1nDb8PA%2Bay0WsldNNHavk1D1D5k4V0LXbi18MclJL2ke1FVvO6gvDXYgFRrBRWh4wPp7z85%2FgA%3D");
            builder.queryParam("RelayState", RELAY_STATE);
            resp.setHeader("Location", builder.build().toString());
            return;
        }
        System.out.println("received response");
        samlResponse = req.getParameter("SAMLResponse");
        sentRelayState = req.getParameter("RelayState");
    }
}
