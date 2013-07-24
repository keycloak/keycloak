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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

import org.keycloak.social.util.UriBuilder;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("")
public class SdkResource {

    @XmlRootElement
    public static class LoginConfig {

        private String callbackUrl;
        
        private String id;

        private String name;

        private String[] providers;

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public String getId() {
            return id;
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

        public void setId(String id) {
            this.id = id;
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

    /**
     * TODO Retrieve configuration for application from IDM
     */
    @GET
    @Path("{application}/login/config")
    @Produces(MediaType.APPLICATION_JSON)
    public LoginConfig getLoginConfig(@PathParam("application") String application) {
        LoginConfig loginConfig = new LoginConfig();
        loginConfig.setId(application);
        loginConfig.setName(application);
        loginConfig.setCallbackUrl("http://localhost:8080");
        loginConfig.setProviders(new String[] { "google", "twitter" });
        return loginConfig;
    }

    @GET
    @Path("{application}/login")
    public Response login(@PathParam("application") String application, @QueryParam("error") String error) {
        UriBuilder ub = new UriBuilder(headers, uriInfo, "sdk/login.html").setQueryParam("application", application);
        if (error != null) {
            ub.setQueryParam("error", error);
        }
        return Response.seeOther(ub.build()).build();
    }

    @GET
    @Path("{application}/register")
    public Response register(@PathParam("application") String application, @QueryParam("error") String error) {
        UriBuilder ub = new UriBuilder(headers, uriInfo, "sdk/register.html").setQueryParam("application", application);
        if (error != null) {
            ub.setQueryParam("error", error);
        }
        return Response.seeOther(ub.build()).build();
    }

}

