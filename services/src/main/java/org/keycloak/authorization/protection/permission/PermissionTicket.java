/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.keycloak.authorization.protection.permission;

import org.keycloak.TokenIdGenerator;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionTicket extends JsonWebToken {

    private final List<ResourceRepresentation> resources = new ArrayList<>();
    private final String resourceServerId;

    public PermissionTicket() {
        this.resourceServerId = null;
    }

    public PermissionTicket(List<ResourceRepresentation> resources, String resourceServerId, AccessToken accessToken) {
        id(TokenIdGenerator.generateId());
        subject(accessToken.getSubject());
        expiration(accessToken.getExpiration());
        notBefore(accessToken.getNotBefore());
        issuedAt(accessToken.getIssuedAt());
        issuedFor(accessToken.getIssuedFor());
        this.resources.addAll(resources);
        this.resourceServerId = resourceServerId;
    }

    public List<ResourceRepresentation> getResources() {
        return this.resources;
    }

    public String getResourceServerId() {
        return this.resourceServerId;
    }
}
