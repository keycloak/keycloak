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

    private String getSamlRequest() {
        if (System.getProperty("auth.server.ssl.required", "false").equals("true")) {
            return "jZJJT8MwEIXvSPyHyPfUTrokWE2lQoWoxBLRwoELMs6UWnLs4HFY%2Fj1OoALEIiQfLPt55r1vPEVR64bPW781l%2FDQAvroudYGeX9RkNYZbgUq5EbUgNxLvpqfnfJ0wHjjrLfSavL25G%2BxQATnlTUkmu%2B2R9ZgW4NbgXtUEq4uTwuy9b5BTqm2UuitRc%2FzyWhIoW60fQGgJFoEk8qIrsCP8nGQixCIOhC6RlpBbenOK%2B1ykejYOgl96oJshEYg0XJRkOXidlTlMJaQxlnKJvEoS%2B9ikR9kcVrlw4wdMMnYOIixDIHUI3w8R2xhadAL4wuSsmQcs2HMJus05WEl2SAYuyFR%2BW7kUJlKmfu%2Fod29iZCfrNdlXF6s1iS6Bod99CAgs%2F29aZeI9%2B3dp9n9dxqzjuBXgCxPPnhP6af6u3YNPw8ll4vSaiVfornW9uko4PaBh3ct9IBr4X83kQyS%2FkRV8aaX8tZgA1JtFFSEdo3o9785ewU%3D";
        }

        return "jZJdS8MwFIbvBf9DyX2XNG62hnUwHeLAj7JNL7yRmJ65QJrUnNSPf29WHQp%2BIOQiJM%2FJed%2F3ZIyyMa2YdmFjF%2FDYAYbkpTEWRX9Rks5b4SRqFFY2gCIosZxenAs%2BYKL1LjjlDHkv%2BRuWiOCDdpYk0932xFnsGvBL8E9awfXivCSbEFpBqXFKmo3DIApeMApNa9wrACXJLGrUVm7rf6KzSMtoh3qQpkFaQ%2BPoTinduiLJqfMKes8lWUuDQJL5rCTz2d2wLmCkgKc5Z4fpMOf3qSyO8pTXxUHOjphibBRhrKId%2FQSf5YgdzC0GaUNJOMtGKTtI2eGKcxFXlg%2BK0fCWJNWHkGNta20f%2Fo7s%2Fh1CcbZaVWl1tVyR5AY89s4jQCb7e%2BOtI9G3918m999ZTL4HyIrsM%2B4x%2FfL%2Brl0rLuOT81nljFavydQY93wS4w4xj%2BA76ANuZPhdRDbI%2BhNdp%2BseFZ3FFpRea6gJ3Tai33%2Fm5A0%3D";
    }
}
