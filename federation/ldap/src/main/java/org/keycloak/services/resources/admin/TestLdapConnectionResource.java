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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.TestLdapConnectionRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.LDAPServerCapabilitiesManager;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource User Storage Provider
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TestLdapConnectionResource {
    protected final RealmModel realm;

    protected final AdminPermissionEvaluator auth;

    protected final KeycloakSession session;

    public TestLdapConnectionResource(KeycloakSession session, AdminPermissionEvaluator auth) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
    }

    /**
     * Test LDAP connection
     *
     * @param action
     * @param connectionUrl
     * @param bindDn
     * @param bindCredential
     * @return
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Deprecated
    public Response testLDAPConnection(@FormParam("action") String action, @FormParam("connectionUrl") String connectionUrl,
                                       @FormParam("bindDn") String bindDn, @FormParam("bindCredential") String bindCredential,
                                       @FormParam("useTruststoreSpi") String useTruststoreSpi, @FormParam("connectionTimeout") String connectionTimeout,
                                       @FormParam("componentId") String componentId, @FormParam("startTls") String startTls) {
        auth.realm().requireManageRealm();

        TestLdapConnectionRepresentation config = new TestLdapConnectionRepresentation(action, connectionUrl, bindDn, bindCredential, useTruststoreSpi, connectionTimeout, startTls, LDAPConstants.AUTH_TYPE_SIMPLE);
        config.setComponentId(componentId);
        try {
            LDAPServerCapabilitiesManager.testLDAP(config, session, realm);
            return Response.noContent().build();
        } catch(Exception e) {
            String errorMsg = LDAPServerCapabilitiesManager.getErrorCode(e);
            throw ErrorResponse.error(errorMsg, Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Test LDAP connection
     * @return
     */
    @POST
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response testLDAPConnection(TestLdapConnectionRepresentation config) {
        auth.realm().requireManageRealm();
        try {
            LDAPServerCapabilitiesManager.testLDAP(config, session, realm);
            return Response.noContent().build();
        } catch(Exception e) {
            String errorMsg = LDAPServerCapabilitiesManager.getErrorCode(e);
            throw ErrorResponse.error(errorMsg, Response.Status.BAD_REQUEST);
        }
    }

}
