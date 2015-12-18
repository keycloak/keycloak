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

        dn.addFirst("uid", "Johny,Depp");
        Assert.assertEquals("uid=Johny\\,Depp,ou=People,dc=keycloak,dc=org", dn.toString());

        Assert.assertEquals("ou=People,dc=keycloak,dc=org", dn.getParentDn());

        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("dc=keycloak, dc=org")));
        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("dc=org")));
        Assert.assertTrue(dn.isDescendantOf(LDAPDn.fromString("DC=keycloak, DC=org")));
        Assert.assertFalse(dn.isDescendantOf(LDAPDn.fromString("dc=keycloakk, dc=org")));
        Assert.assertFalse(dn.isDescendantOf(dn));

        Assert.assertEquals("uid", dn.getFirstRdnAttrName());
        Assert.assertEquals("Johny\\,Depp", dn.getFirstRdnAttrValue());
    }
}
