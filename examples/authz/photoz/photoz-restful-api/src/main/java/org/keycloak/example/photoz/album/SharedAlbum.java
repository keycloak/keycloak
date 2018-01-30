/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.example.photoz.album;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.example.photoz.entity.Album;

public class SharedAlbum {

    private Album album;
    private List<String> scopes;

    public SharedAlbum(Album album) {
        this.album = album;
    }

    public Album getAlbum() {
        return album;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void addScope(String scope) {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        scopes.add(scope);
    }
}
