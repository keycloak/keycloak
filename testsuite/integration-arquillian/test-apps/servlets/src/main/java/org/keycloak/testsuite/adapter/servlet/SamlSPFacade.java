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
            // UriBuilder builder = UriBuilder.fromUri("http://localhost:8081/auth/realms/demo/protocol/saml?SAMLRequest=jVLRTsIwFP2Vpe%2BjG4wxG0YyWYxL0BBAH3wx3XYnTbp29nYof%2B8YEvEBNOlD03vOveec2ynyWjYsae1WreC9BbTOZy0Vsr4Qk9YopjkKZIrXgMwWbJ08LNhw4LHGaKsLLcmRch3MEcFYoRVxktN1rhW2NZg1mJ0o4Gm1iMnW2oZRKnXB5VajZZEX%2BRTqRuo9ACVO2mkUih%2F4l9C8s0MNcFkjLaHW9KSUHlwR506bAnrPMam4RCBOlsYkS1%2BD3MvLcDJxAx9KN4jCkXszrG5cP%2BCVH4y8IM8PYFx2dsQOfuiILWQKLVc2JkPPH7te6HrRxh%2BzUdidwSSIXoiz%2FBZyK1Qp1Nv1yPIjCNn9ZrN0V1AKA4UlzjMY7N13IDKbHjyxXoA5291%2FtzH7I%2FApPet%2FHNawx65hli61FMXeSaTUH%2FMubtvlYU0LfcA1t5cl%2BAO%2FfxGlW%2FVQ1ipsoBCVgJLQ2XHo7385%2BwI%3D");
            UriBuilder builder = UriBuilder.fromUri("http://localhost:8180/auth/realms/demo/protocol/saml?SAMLRequest=jZJdS8MwFIbvBf9DyX2XNG62hnUwHeLAj7JNL7yRmJ65QJrUnNSPf29WHQp%2BIOQiJM%2FJed%2F3ZIyyMa2YdmFjF%2FDYAYbkpTEWRX9Rks5b4SRqFFY2gCIosZxenAs%2BYKL1LjjlDHkv%2BRuWiOCDdpYk0932xFnsGvBL8E9awfXivCSbEFpBqXFKmo3DIApeMApNa9wrACXJLGrUVm7rf6KzSMtoh3qQpkFaQ%2BPoTinduiLJqfMKes8lWUuDQJL5rCTz2d2wLmCkgKc5Z4fpMOf3qSyO8pTXxUHOjphibBRhrKId%2FQSf5YgdzC0GaUNJOMtGKTtI2eGKcxFXlg%2BK0fCWJNWHkGNta20f%2Fo7s%2Fh1CcbZaVWl1tVyR5AY89s4jQCb7e%2BOtI9G3918m999ZTL4HyIrsM%2B4x%2FfL%2Brl0rLuOT81nljFavydQY93wS4w4xj%2BA76ANuZPhdRDbI%2BhNdp%2BseFZ3FFpRea6gJ3Tai33%2Fm5A0%3D");
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
}
