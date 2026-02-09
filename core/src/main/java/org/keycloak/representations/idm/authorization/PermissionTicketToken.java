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
package org.keycloak.representations.idm.authorization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.TokenIdGenerator;
import org.keycloak.json.StringListMapDeserializer;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PermissionTicketToken extends JsonWebToken {

    private final List<Permission> permissions;

    @JsonDeserialize(using = StringListMapDeserializer.class)
    private Map<String, List<String>> claims;

    public PermissionTicketToken() {
        this(new ArrayList<Permission>());
    }

    public PermissionTicketToken(List<Permission> permissions, String audience, AccessToken accessToken) {
        if (accessToken != null) {
            id(TokenIdGenerator.generateId());
            subject(accessToken.getSubject());
            this.exp(accessToken.getExp());
            this.nbf(accessToken.getNbf());
            iat(accessToken.getIat());
            issuedFor(accessToken.getIssuedFor());
        }
        if (audience != null) {
            audience(audience);
        }
        this.permissions = permissions;
    }

    public PermissionTicketToken(List<Permission> resources) {
        this(resources, null, null);
    }

    public List<Permission> getPermissions() {
        return this.permissions;
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, List<String>> claims) {
        this.claims = claims;
    }
}
