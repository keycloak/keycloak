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
package org.keycloak.services.resources.admin;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.LDAPServerCapabilitiesManager;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @resource User Storage Provider
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LdapServerCapabilitiesResource {

    protected final RealmModel realm;

    protected final AdminPermissionEvaluator auth;

    protected final AdminEventBuilder adminEvent;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;

    public LdapServerCapabilitiesResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent;
        this.clientConnection = session.getContext().getConnection();
        this.headers = session.getContext().getRequestHeaders();
    }

    /**
     * Get LDAP supported extensions.
     * @param config LDAP configuration
     * @return
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response ldapServerCapabilities(TestLdapConnectionRepresentation config) {
        auth.realm().requireManageRealm();
        try {
            Set<LDAPCapabilityRepresentation> ldapCapabilities = LDAPServerCapabilitiesManager.queryServerCapabilities(config, session, realm);
            return Response.ok().entity(ldapCapabilities).build();
        } catch (Exception e) {
            return ErrorResponse.error("ldapServerCapabilities error", Response.Status.BAD_REQUEST);
        }
    }

}
