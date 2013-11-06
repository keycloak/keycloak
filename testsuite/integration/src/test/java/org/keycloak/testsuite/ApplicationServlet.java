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

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String title = "";
        String body = "";

        StringBuffer sb = req.getRequestURL();
        sb.append("?");
        sb.append(req.getQueryString());

        List<NameValuePair> query = null;

        try {
            query = URLEncodedUtils.parse(new URI(sb.toString()), "UTF-8");
        } catch (URISyntaxException e) {
            throw new ServletException(e);
        }

        if (req.getRequestURI().endsWith("auth")) {
            title = "AUTH_RESPONSE";
        } else if (req.getRequestURI().endsWith("logout")) {
            title = "LOGOUT_REQUEST";
        } else {
            title = "APP_REQUEST";
        }

        PrintWriter pw = resp.getWriter();
        pw.printf("<html><head><title>%s</title></head><body>%s</body>", title, body);
        pw.flush();
    }

}
