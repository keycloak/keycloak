/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.photoz.album;

import org.keycloak.example.photoz.CustomDatabase;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.security.Principal;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/profile")
public class ProfileService {

    private static final String PROFILE_VIEW = "profile:view";

    private CustomDatabase customDatabase = CustomDatabase.create();

    @GET
    @Produces("application/json")
    public Response view(@Context HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        List albums = this.customDatabase.findByUserId(userPrincipal.getName());
        return Response.ok(new Profile(userPrincipal.getName(), albums.size())).build();
    }

    public static class Profile {
        private String userName;
        private int totalAlbums;

        public Profile(String name, int totalAlbums) {
            this.userName = name;
            this.totalAlbums = totalAlbums;
        }

        public String getUserName() {
            return userName;
        }

        public int getTotalAlbums() {
            return totalAlbums;
        }
    }
}
