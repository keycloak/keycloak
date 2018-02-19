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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.keycloak.example.photoz.entity.Album;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@Path("/admin/album")
public class AdminAlbumService {

    @Inject
    private EntityManager entityManager;

    @GET
    @Produces("application/json")
    public Response findAll() {
        HashMap<String, List<Album>> albums = new HashMap<>();
        List<Album> result = this.entityManager.createQuery("from Album").getResultList();

        for (Album album : result) {
            List<Album> userAlbums = albums.get(album.getUserId());

            if (userAlbums == null) {
                userAlbums = new ArrayList<>();
                albums.put(album.getUserId(), userAlbums);
            }

            userAlbums.add(album);
        }

        return Response.ok(albums).build();
    }
}
