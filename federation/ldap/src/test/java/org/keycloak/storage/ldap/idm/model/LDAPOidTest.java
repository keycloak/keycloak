package org.keycloak.storage.ldap.idm.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import org.keycloak.storage.ldap.idm.store.ldap.extended.PasswordModifyRequest;

public class LDAPOidTest {

  @Test
  public void testEquals() {
    LDAPOid oid1 = new LDAPOid(PasswordModifyRequest.PASSWORD_MODIFY_OID);
    LDAPOid oid2 = new LDAPOid(PasswordModifyRequest.PASSWORD_MODIFY_OID);
    assertTrue(oid1.equals(oid2));
    System.out.println(oid1);
  }

  @Test
  public void testContains() {
    LDAPOid oid1 = new LDAPOid(PasswordModifyRequest.PASSWORD_MODIFY_OID);
    LDAPOid oidx = new LDAPOid(PasswordModifyRequest.PASSWORD_MODIFY_OID);
    LDAPOid oid2 = new LDAPOid("13.2.3.11.22");
    LDAPOid oid3 = new LDAPOid("14.2.3.42.22");
    Set<LDAPOid> ids = new LinkedHashSet<>();
    ids.add(oid1);
    ids.add(oidx);
    ids.add(oid2);
    ids.add(oid3);
    assertTrue(ids.contains(oid1));
    assertTrue(ids.contains(oidx));
    assertEquals(3, ids.size());
  }

}
