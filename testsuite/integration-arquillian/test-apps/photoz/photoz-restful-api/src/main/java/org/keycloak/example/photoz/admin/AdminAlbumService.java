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
package org.keycloak.example.photoz.admin;

import org.keycloak.example.photoz.CustomDatabase;
import org.keycloak.example.photoz.entity.Album;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/admin/album")
public class AdminAlbumService {

    public static final String SCOPE_ADMIN_ALBUM_MANAGE = "admin:manage";

    private CustomDatabase entityManager = CustomDatabase.create();

    @Context
    private HttpHeaders headers;

    @GET
    @Produces("application/json")
    public Response findAll() {
        HashMap<String, List<Album>> albums = new HashMap<>();
        List<Album> result = this.entityManager.getAll();

        for (Album album : result) {
            //We need to compile this under JDK7 so we can't use lambdas
            //albums.computeIfAbsent(album.getUserId(), key -> new ArrayList<>()).add(album);

            if (!albums.containsKey(album.getUserId())) {
                albums.put(album.getUserId(), Collections.singletonList(album));
            }
        }

        return Response.ok(albums).build();
    }
}
