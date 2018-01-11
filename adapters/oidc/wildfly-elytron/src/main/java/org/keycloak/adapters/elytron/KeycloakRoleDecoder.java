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
package org.keycloak.adapters.elytron;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.util.JsonSerialization;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.Attributes.Entry;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;

public class KeycloakRoleDecoder implements RoleDecoder {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_RESOURCE_ACCESS = "resource_access";
    private static final String CLAIM_ROLES = "roles";

    @Override
    public Roles decodeRoles(AuthorizationIdentity identity) {
        Attributes attributes = identity.getAttributes();
        Entry realmAccess = attributes.get(CLAIM_REALM_ACCESS);
        Set<String> roleSet = new HashSet<>();

        if (realmAccess != null) {
            String realmAccessValue = realmAccess.get(0);

            try {
                Map<String, List<String>> jsonNode = JsonSerialization.readValue(realmAccessValue, Map.class);
                List<String> roles = jsonNode.get(CLAIM_ROLES);

                if (roles != null) {
                    roleSet.addAll(roles);
                }
            } catch (IOException cause) {
                throw new RuntimeException("Failed to decode realm access roles", cause);
            }
        }

        Entry resourceAccess = attributes.get(CLAIM_RESOURCE_ACCESS);

        if (resourceAccess != null) {
            Iterator<String> iterator = resourceAccess.iterator();

            while (iterator.hasNext()) {
                try {
                    Map<String, Map<String, List<String>>> resources = JsonSerialization.readValue(iterator.next(), Map.class);

                    for (String resourceKey : resources.keySet()) {
                        List<String> roles = resources.get(resourceKey).get("roles");

                        if (roles != null) {
                            roleSet.addAll(roles);
                        }
                    }
                } catch (IOException cause) {
                    throw new RuntimeException("Failed to decode resource access roles", cause);
                }
            }
        }

        return Roles.fromSet(roleSet);
    }
}
