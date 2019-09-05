/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import javax.ws.rs.core.Response;
import static org.keycloak.testsuite.admin.ApiUtil.getCreatedId;

/**
 *  Creates a temporary realm object and makes sure it is removed when used within try-with-resources.
 */
public class Creator<T> implements AutoCloseable {

    public static Creator<RealmResource> create(Keycloak adminClient, RealmRepresentation rep) {
        adminClient.realms().create(rep);
        final RealmResource r = adminClient.realm(rep.getRealm());
        return new Creator(r, r::remove);
    }

    public static Creator<GroupResource> create(RealmResource realmResource, GroupRepresentation rep) {
        final GroupsResource groups = realmResource.groups();
        try (Response response = groups.add(rep)) {
            String createdId = getCreatedId(response);
            final GroupResource r = groups.group(createdId);
            return new Creator(r, r::remove);
        }
    }

    public static Creator<UserResource> create(RealmResource realmResource, UserRepresentation rep) {
        final UsersResource users = realmResource.users();
        try (Response response = users.create(rep)) {
            String createdId = getCreatedId(response);
            final UserResource r = users.get(createdId);
            return new Creator(r, r::remove);
        }
    }

    private final T resource;
    private final Runnable closer;

    private Creator(T resource, Runnable closer) {
        this.resource = resource;
        this.closer = closer;
    }

    public T resource() {
        return this.resource;
    }

    @Override
    public void close() {
        closer.run();
    }

}
