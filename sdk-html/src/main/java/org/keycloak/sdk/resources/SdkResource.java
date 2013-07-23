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
package org.keycloak.sdk.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("sdk")
public class SdkResource {

    @XmlRootElement
    public static class LoginConfig {

        private String callbackUrl;

        private String name;

        private String[] providers;

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public String getName() {
            return name;
        }

        public String[] getProviders() {
            return providers;
        }

        public void setCallbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setProviders(String[] providers) {
            this.providers = providers;
        }

    }

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("config/{application}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("application") String application) {
        String applicationCallbackUrl = null; // TODO Get application callback url
        String applicationJavaScriptOrigin = null; // TODO Get application javascript origin
        
        LoginConfig loginConfig = new LoginConfig();
        loginConfig.setName(application);
        loginConfig.setCallbackUrl(applicationCallbackUrl);
        loginConfig.setProviders(null); // TODO Get configured identity providers for application

        ResponseBuilder response = Response.ok(loginConfig);

        if (applicationJavaScriptOrigin != null) {
            response.header("Access-Control-Allow-Origin", applicationJavaScriptOrigin);
        }

        return response.build();
    }

    @GET
    @Path("login/{application}")
    @Produces(MediaType.TEXT_HTML)
    public Response login(@PathParam("application") String application) {
        return Response.ok(getClass().getResourceAsStream("login.html")).build();
    }

    @GET
    @Path("register/{application}")
    @Produces(MediaType.TEXT_HTML)
    public Response register(@PathParam("application") String application) {
        return Response.ok(getClass().getResourceAsStream("register.html")).build();
    }
}
