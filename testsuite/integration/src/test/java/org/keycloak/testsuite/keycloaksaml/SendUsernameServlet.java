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

import org.junit.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.List;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public class SendUsernameServlet extends HttpServlet {

    public static Principal sentPrincipal;
    public static List<String> checkRoles;

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doGet()");
        if (checkRoles != null) {
            for (String role : checkRoles) {
                System.out.println("check role: " + role);
                //Assert.assertTrue(req.isUserInRole(role));
                if (!req.isUserInRole(role)) {
                    resp.sendError(403);
                    return;
                }
            }

        }
        resp.setContentType("text/plain");
        OutputStream stream = resp.getOutputStream();
        Principal principal = req.getUserPrincipal();
        stream.write("request-path: ".getBytes());
        if (req.getPathInfo() != null) stream.write(req.getPathInfo().getBytes());
        stream.write("\n".getBytes());
        stream.write("principal=".getBytes());
        if (principal == null) {
            stream.write("null".getBytes());
            return;
        }
        String name = principal.getName();
        stream.write(name.getBytes());
        sentPrincipal = principal;

    }
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doPost()");
        if (checkRoles != null) {
            for (String role : checkRoles) {
                System.out.println("check role: " + role);
                Assert.assertTrue(req.isUserInRole(role));
            }

        }
        resp.setContentType("text/plain");
        OutputStream stream = resp.getOutputStream();
        Principal principal = req.getUserPrincipal();
        stream.write("request-path: ".getBytes());
        stream.write(req.getPathInfo().getBytes());
        stream.write("\n".getBytes());
        stream.write("principal=".getBytes());
        if (principal == null) {
            stream.write("null".getBytes());
            return;
        }
        String name = principal.getName();
        stream.write(name.getBytes());
        sentPrincipal = principal;
    }
}
