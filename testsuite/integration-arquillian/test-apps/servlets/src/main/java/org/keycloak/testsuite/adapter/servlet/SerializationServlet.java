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

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.PrintWriter;

/**
 * @author mhajas
 */
@WebServlet("/serialization-servlet")
public class SerializationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter pw = resp.getWriter();
        // Serialize
        ByteArrayOutputStream bso = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bso);
        oos.writeObject(req.getUserPrincipal());
        oos.close();

        // Deserialize
        byte[] bytes = bso.toByteArray();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bis) {
            @Override
            public Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                try {
                    return Class.forName(desc.getName(), true, SerializationServlet.class.getClassLoader());
                } catch (Exception e) { }

                // Fall back (e.g. for primClasses)
                return super.resolveClass(desc);
            }
        };

        KeycloakPrincipal principal;
        try {
            principal = (KeycloakPrincipal) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            pw.write("Deserialization failed");
            return;
        }

        KeycloakSecurityContext ctx = principal.getKeycloakSecurityContext();
        if (!(ctx instanceof RefreshableKeycloakSecurityContext)) {
            pw.write("Context was not instance of RefreshableKeycloakSecurityContext");
        }
        
        pw.write("Serialization/Deserialization was successful");
    }
}
