/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.admin.client;

import javax.ws.rs.core.Response;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractClientTest extends AbstractAuthTest {

    protected RealmRepresentation realmRep() {
        return testRealmResource().toRepresentation();
    }

    protected void createOidcClient(String name) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(name);
        clientRep.setName(name);
        clientRep.setRootUrl("foo");
        clientRep.setProtocol("openid-connect");
        createClient(clientRep);
    }

        protected void createSamlClient(String name) {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(name);
        clientRep.setName(name);
        clientRep.setProtocol("saml");
        clientRep.setAdminUrl("samlEndpoint");
        createClient(clientRep);
    }

    protected void createClient(ClientRepresentation clientRep) {
        Response resp = testRealmResource().clients().create(clientRep);
        // for some reason, findAll() will later fail unless readEntity is called here
        resp.readEntity(String.class);
        //testRealmResource().clients().findAll();
    }

    protected ClientRepresentation findClientRepresentation(String name) {
        ClientResource clientRsc = findClientResource(name);
        if (clientRsc == null) return null;
        return findClientResource(name).toRepresentation();
    }

    protected ClientResource findClientResource(String name) {
        return ApiUtil.findClientResourceByName(testRealmResource(), name);
    }

}
