/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite;

import org.keycloak.services.resources.RealmsResource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationServlet extends HttpServlet {

    private static final String LINK = "<a href=\"%s\" id=\"%s\">%s</a>";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String title = "";
        if (req.getRequestURI().endsWith("auth")) {
            title = "AUTH_RESPONSE";
        } else if (req.getRequestURI().endsWith("logout")) {
            title = "LOGOUT_REQUEST";
        } else {
            title = "APP_REQUEST";
        }

        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>", title);
        UriBuilder base = UriBuilder.fromUri("http://localhost:8081/auth");
        pw.printf(LINK, RealmsResource.accountUrl(base), "account", "account");

        pw.print("</body></html>");
        pw.flush();
    }

}
