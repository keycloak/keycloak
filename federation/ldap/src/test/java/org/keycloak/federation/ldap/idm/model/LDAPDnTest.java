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

package org.keycloak.federation.ldap.idm.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LDAPDnTest {

    @Test
    public void testDn() throws Exception {
        LDAPDn dn = LDAPDn.fromString("dc=keycloak, dc=org");
        dn.addFirst("ou", "People");
        Assert.assertEquals("ou=People,dc=keycloak,dc=org", dn.toString());

        dn.addFirst("uid", "Johny,Depp+Pepp");
        Assert.assertEquals("uid=Johny\\,Depp\\+Pepp,ou=People,dc=keycloak,dc=org", dn.toString());
        Assert.assertEquals(LDAPDn.fromString("uid=Johny\\,Depp\\+Pepp,ou=People,dc=keycloak,dc=org"), dn);

        Assert.assertEquals("ou=People,dc=keycloak,dc=org", dn.getParentDn());

        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("dc=keycloak, dc=org")));
        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("dc=org")));
        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("DC=keycloak, DC=org")));
        Assert.assertFalse(dn.isDescendantOf(LDAPDn.fromString("dc=keycloakk, dc=org")));
        Assert.assertFalse(dn.isDescendantOf(dn));

        Assert.assertEquals("uid", dn.getFirstRdnAttrName());
        Assert.assertEquals("Johny\\,Depp\\+Pepp", dn.getFirstRdnAttrValue());
    }
}
