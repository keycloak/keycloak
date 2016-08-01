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


import org.jboss.resteasy.annotations.cache.NoCache;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.Principal;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author mhajas
 * @version $Revision: 1 $
 */
@Path("/")
public class SendUsernameServlet {

    private static boolean checkRoles = false;

    @Context
    private HttpServletRequest httpServletRequest;

    @GET
    @NoCache
    public Response doGet(@QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doGet() check roles is " + (checkRolesFlag || checkRoles));
        if (httpServletRequest.getUserPrincipal() != null && (checkRolesFlag || checkRoles) && !checkRoles()) {
            return Response.status(Response.Status.FORBIDDEN).entity("Forbidden").build();
        }

        return Response.ok(getOutput(), MediaType.TEXT_PLAIN).build();
    }

    @POST
    @NoCache
    public Response doPost(@QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doPost() check roles is " + (checkRolesFlag || checkRoles));

        if (httpServletRequest.getUserPrincipal() != null && (checkRolesFlag || checkRoles) && !checkRoles()) {
            throw new RuntimeException("User: " + httpServletRequest.getUserPrincipal() + " do not have required role");
        }

        return Response.ok(getOutput(), MediaType.TEXT_PLAIN).build();
    }

    @GET
    @Path("{path}")
    public Response doGetElseWhere(@PathParam("path") String path, @QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doGetElseWhere() - path: " + path);
        return doGet(checkRolesFlag);
    }

    @POST
    @Path("{path}")
    public Response doPostElseWhere(@PathParam("path") String path, @QueryParam("checkRoles") boolean checkRolesFlag) throws ServletException, IOException {
        System.out.println("In SendUsername Servlet doPostElseWhere() - path: " + path);
        return doPost(checkRolesFlag);
    }

    @GET
    @Path("checkRoles")
    public String checkRolesEndPoint() {
        checkRoles = true;
        System.out.println("Setting checkRoles to true");
        return "Roles will be checked";
    }

    private boolean checkRoles() {
        return httpServletRequest.isUserInRole("manager");
    }

    private String getOutput() {
        String output = "request-path: ";
        output += httpServletRequest.getServletPath();
        output += "\n";
        output += "principal=";
        Principal principal = httpServletRequest.getUserPrincipal();

        if (principal == null) {
            return output + "null";
        }

        return output + principal.getName();
    }
}
