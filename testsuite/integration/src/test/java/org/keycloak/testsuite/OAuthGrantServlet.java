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

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.PemUtils;
import org.keycloak.RSATokenVerifier;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.servlet.ServletOAuthClient;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.util.Map;

/**
 * @author <a href="mailto:vrockai@redhat.com">Viliam Rockai</a>
 */
public class OAuthGrantServlet extends HttpServlet {

    public static ServletOAuthClient client;

    private static String baseUrl = Constants.AUTH_SERVER_ROOT + "/rest";
    private static String realm = "test";

    private static String realmKeyPem = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvg" +
            "cwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/" +
            "p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB";

    public void init() {
        client = new ServletOAuthClient();
        client.setClientId("third-party");
        client.setPassword("password");
        client.setAuthUrl(UriBuilder.fromUri(baseUrl + "/realms/" + realm + "/tokens/login").build().toString());
        client.setCodeUrl(UriBuilder.fromUri(baseUrl + "/realms/" + realm + "/tokens/access/codes").build().toString());
        client.setClient(new ResteasyClientBuilder().build());
        client.start();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{

        PrintWriter pw = resp.getWriter();

        // Error "access_denied" happens after clicking on cancel when asked for granting permission
        if (req.getParameterValues("error") != null){
            pw.print("<html><head><title></title></head><body>Access rights not granted.</body></html>");

        // Code is sent as a parameter in case that access was granted
        } else if(req.getParameterValues("code") != null) {
            String token = client.getBearerToken(req);

            pw.print("<html><head><title></title></head><body>Access rights granted.<br/>Token:"+token+"<br/>");

            // Check whether the token itself is relevant
            try {
                PublicKey realmKey = PemUtils.decodePublicKey(realmKeyPem);
                SkeletonKeyToken skeletonToken = RSATokenVerifier.verifyToken(token, realmKey, realm);

                // Writing app/role information on a page in format which is easy to assert in a test.
                pw.print("Role:");
                for(String role: skeletonToken.getRealmAccess().getRoles()){
                    pw.print(role);
                }
                pw.print(".<br/>");

                for(Map.Entry<String, SkeletonKeyToken.Access> entry: skeletonToken.getResourceAccess().entrySet()){
                    pw.print("App:"+entry.getKey()+";");
                    for(String role: entry.getValue().getRoles()){
                        pw.print(role);
                    }
                }
                pw.print(".<br/>");
            } catch (Exception e){
            }

            pw.print("</body></html>");

        // If no code was sent or error happened, it's 1st visit to servlet and we need to ask for permissions
        } else  {
            client.redirectRelative("", req, resp);
        }

        pw.flush();
    }

}
