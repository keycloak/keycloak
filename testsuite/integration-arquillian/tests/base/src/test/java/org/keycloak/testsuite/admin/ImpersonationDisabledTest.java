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

import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Assume;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

/**
 * @author <a href="mailto:vramik@redhat.com">Vlastislav Ramik</a>
 */
public class ImpersonationDisabledTest extends AbstractAdminTest {
    
    public static boolean IMPERSONATION_DISABLED = "impersonation".equals(System.getProperty("feature.name")) 
                && "disabled".equals(System.getProperty("feature.value"));

    @BeforeClass
    public static void enabled() {
        Assume.assumeTrue(IMPERSONATION_DISABLED);
    }

    @Test
    public void testImpersonationDisabled() {
        String impersonatedUserId = adminClient.realm(TEST).users().search("test-user@localhost", 0, 1).get(0).getId();
        
        try {
            log.debug("--Expected javax.ws.rs.WebApplicationException--");
            adminClient.realms().realm("test").users().get(impersonatedUserId).impersonate();
        } catch (ServerErrorException e) {
            assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), e.getResponse().getStatus());
            return;
        }
        fail("Feature impersonation should be disabled.");
    }

}
