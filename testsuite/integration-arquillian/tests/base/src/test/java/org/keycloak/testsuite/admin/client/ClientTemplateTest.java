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

package org.keycloak.testsuite.admin.client;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientTemplateTest extends AbstractClientTest {


    // KEYCLOAK-2809
    @Test
    public void testRemove() {
        // Add realm role
        RoleRepresentation roleRep = new RoleRepresentation();
        roleRep.setName("foo-role");
        testRealmResource().roles().create(roleRep);
        roleRep = testRealmResource().roles().get("foo-role").toRepresentation();

        // Add client template
        ClientTemplateRepresentation templateRep = new ClientTemplateRepresentation();
        templateRep.setName("bar-template");
        templateRep.setFullScopeAllowed(false);
        Response resp = testRealmResource().clientTemplates().create(templateRep);
        resp.close();
        String clientTemplateId = ApiUtil.getCreatedId(resp);

        // Add realm role to scopes of clientTemplate
        testRealmResource().clientTemplates().get(clientTemplateId).getScopeMappings().realmLevel().add(Collections.singletonList(roleRep));

        List<RoleRepresentation> roleReps = testRealmResource().clientTemplates().get(clientTemplateId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(1, roleReps.size());
        Assert.assertEquals("foo-role", roleReps.get(0).getName());

        // Remove realm role
        testRealmResource().roles().deleteRole("foo-role");

        // Get scope mappings
        roleReps = testRealmResource().clientTemplates().get(clientTemplateId).getScopeMappings().realmLevel().listAll();
        Assert.assertEquals(0, roleReps.size());
    }

}
