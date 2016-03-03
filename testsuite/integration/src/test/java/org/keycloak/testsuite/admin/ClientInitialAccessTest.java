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

package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientInitialAccessResource;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessTest extends AbstractClientTest {

    @Test
    public void create() {
        ClientInitialAccessResource resource = keycloak.realm(REALM_NAME).clientInitialAccess();

        ClientInitialAccessPresentation access = resource.create(new ClientInitialAccessCreatePresentation(1000, 2));
        Assert.assertEquals(new Integer(2), access.getCount());
        Assert.assertEquals(new Integer(2), access.getRemainingCount());
        Assert.assertEquals(new Integer(1000), access.getExpiration());
        Assert.assertNotNull(access.getTimestamp());
        Assert.assertNotNull(access.getToken());

        ClientInitialAccessPresentation access2 = resource.create(new ClientInitialAccessCreatePresentation());

        List<ClientInitialAccessPresentation> list = resource.list();
        Assert.assertEquals(2, list.size());

        for (ClientInitialAccessPresentation r : list) {
            if (r.getId().equals(access.getId())) {
                Assert.assertEquals(new Integer(2), r.getCount());
                Assert.assertEquals(new Integer(2), r.getRemainingCount());
                Assert.assertEquals(new Integer(1000), r.getExpiration());
                Assert.assertNotNull(r.getTimestamp());
                Assert.assertNull(r.getToken());
            } else if(r.getId().equals(access2.getId())) {
                Assert.assertEquals(new Integer(1), r.getCount());
                Assert.assertEquals(new Integer(1), r.getRemainingCount());
                Assert.assertEquals(new Integer(0), r.getExpiration());
                Assert.assertNotNull(r.getTimestamp());
                Assert.assertNull(r.getToken());
            } else {
                Assert.fail("Unexpected id");
            }
        }

        resource.delete(access.getId());
        resource.delete(access2.getId());

        Assert.assertTrue(resource.list().isEmpty());
    }

}
