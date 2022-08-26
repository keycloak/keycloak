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

package org.keycloak.testsuite.client.resources;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.keycloak.utils.MediaType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface TestingLDAPResource {


    /**
     * @param ldapCfg configuration of LDAP provider
     * @param importEnabled specify if LDAP provider will have import enabled
     * @return ID of newly created provider
     */
    @POST
    @Path("/create-ldap-provider")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    String createLDAPProvider(Map<String,String> ldapCfg, @QueryParam("import") boolean importEnabled);


    /**
     * Prepare groups LDAP tests. Creates some LDAP mappers as well as some built-in GRoups and users in LDAP
     */
    @POST
    @Path("/configure-groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    void prepareGroupsLDAPTest();

    /**
     * Prepare hardcoded groups LDAP tests. Creates some LDAP mappers as well as some built-in Groups and users in LDAP
     */
    @POST
    @Path("/configure-hardcoded-groups")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    void prepareHardcodedGroupsLDAPTest();

    /**
     * Prepare groups LDAP tests. Creates some LDAP mappers as well as some built-in Groups and users in LDAP
     */
    @POST
    @Path("/configure-roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    void prepareRolesLDAPTest();

    /**
     * Prepare hardcoded roles LDAP tests. Creates some LDAP mappers as well as some hardcoded roles and users in LDAP
     */
    @POST
    @Path("/configure-hardcoded-roles")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    void prepareHardcodedRolesLDAPTest();

    /**
     * Remove specified user directly just from the LDAP server
     */
    @DELETE
    @Path("/remove-ldap-user")
    @Consumes(MediaType.APPLICATION_JSON)
    void removeLDAPUser(@QueryParam("username") String ldapUsername);
}
