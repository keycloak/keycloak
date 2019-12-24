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

package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdapterUtils {

    private static Logger log = Logger.getLogger(AdapterUtils.class);

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static Set<String> getRolesFromSecurityContext(RefreshableKeycloakSecurityContext session) {
        Set<String> roles = null;
        AccessToken accessToken = session.getToken();
        if (session.getDeployment().isUseResourceRoleMappings()) {
            if (log.isTraceEnabled()) {
                log.trace("useResourceRoleMappings");
            }
            AccessToken.Access access = accessToken.getResourceAccess(session.getDeployment().getResourceName());
            if (access != null) roles = access.getRoles();
        } else {
            if (log.isTraceEnabled()) {
                log.trace("use realm role mappings");
            }
            AccessToken.Access access = accessToken.getRealmAccess();
            if (access != null) roles = access.getRoles();
        }
        if (roles == null) roles = Collections.emptySet();
        if (log.isTraceEnabled()) {
            log.trace("Setting roles: ");
            for (String role : roles) {
                log.trace("   role: " + role);
            }
        }
        return roles;
    }

    public static String getPrincipalName(KeycloakDeployment deployment, AccessToken token) {
        String attr = "sub";
        if (deployment.getPrincipalAttribute() != null) attr = deployment.getPrincipalAttribute();
        String name = null;

        if ("sub".equals(attr)) {
            name = token.getSubject();
        } else if ("email".equals(attr)) {
            name = token.getEmail();
        } else if ("preferred_username".equals(attr)) {
            name = token.getPreferredUsername();
        } else if ("name".equals(attr)) {
            name = token.getName();
        } else if ("given_name".equals(attr)) {
            name = token.getGivenName();
        } else if ("family_name".equals(attr)) {
            name = token.getFamilyName();
        } else if ("nickname".equals(attr)) {
            name = token.getNickName();
        }
        if (name == null) name = token.getSubject();
        return name;
    }

    public static KeycloakPrincipal<RefreshableKeycloakSecurityContext> createPrincipal(KeycloakDeployment deployment, RefreshableKeycloakSecurityContext securityContext) {
        return new KeycloakPrincipal<>(getPrincipalName(deployment, securityContext.getToken()), securityContext);
    }
}
