package org.keycloak.storage.ldap.idm.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import org.keycloak.storage.ldap.idm.model.LDAPCapability.CapabilityType;
import org.keycloak.storage.ldap.idm.store.ldap.extended.PasswordModifyRequest;

public class LDAPCapabilityTest {

  @Test
  public void testEquals() {
    LDAPCapability oid1 = new LDAPCapability(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.CONTROL);
    LDAPCapability oid2 = new LDAPCapability(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
    assertTrue(oid1.equals(oid2));
    System.out.println(oid1);
  }

  @Test
  public void testContains() {
    LDAPCapability oid1 = new LDAPCapability(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
    LDAPCapability oidx = new LDAPCapability(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
    LDAPCapability oid2 = new LDAPCapability("13.2.3.11.22", CapabilityType.CONTROL);
    LDAPCapability oid3 = new LDAPCapability("14.2.3.42.22", CapabilityType.FEATURE);
    Set<LDAPCapability> ids = new LinkedHashSet<>();
    ids.add(oid1);
    ids.add(oidx);
    ids.add(oid2);
    ids.add(oid3);
    assertTrue(ids.contains(oid1));
    assertTrue(ids.contains(oidx));
    assertEquals(3, ids.size());
  }

  @Test
  public void testCapabilityTypeFromAttributeName() {
    CapabilityType extension = CapabilityType.fromRootDseAttributeName("supportedExtension");
    assertEquals(CapabilityType.EXTENSION, extension);

    CapabilityType control = CapabilityType.fromRootDseAttributeName("supportedControl");
    assertEquals(CapabilityType.CONTROL, control);

    CapabilityType feature = CapabilityType.fromRootDseAttributeName("supportedFeatures");
    assertEquals(CapabilityType.FEATURE, feature);

    CapabilityType unknown = CapabilityType.fromRootDseAttributeName("foo");
    assertEquals(CapabilityType.UNKNOWN, unknown);
  }
}
