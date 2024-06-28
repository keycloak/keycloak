package org.keycloak.storage.ldap.idm.model;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation;
import org.keycloak.representations.idm.LDAPCapabilityRepresentation.CapabilityType;
import org.keycloak.storage.ldap.idm.store.ldap.extended.PasswordModifyRequest;

public class LDAPCapabilityTest {

    @Test
    public void testEquals() {
        LDAPCapabilityRepresentation oid1 = new LDAPCapabilityRepresentation(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.CONTROL);
        LDAPCapabilityRepresentation oid2 = new LDAPCapabilityRepresentation(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
        LDAPCapabilityRepresentation oid3 = new LDAPCapabilityRepresentation(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
        assertFalse(oid1.equals(oid2));
        assertTrue(oid2.equals(oid3));
        System.out.println(oid1);
    }

    @Test
    public void testContains() {
        LDAPCapabilityRepresentation oid1 = new LDAPCapabilityRepresentation(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
        LDAPCapabilityRepresentation oidx = new LDAPCapabilityRepresentation(PasswordModifyRequest.PASSWORD_MODIFY_OID, CapabilityType.EXTENSION);
        LDAPCapabilityRepresentation oid2 = new LDAPCapabilityRepresentation("13.2.3.11.22", CapabilityType.CONTROL);
        LDAPCapabilityRepresentation oid3 = new LDAPCapabilityRepresentation("14.2.3.42.22", CapabilityType.FEATURE);
        Set<LDAPCapabilityRepresentation> ids = new LinkedHashSet<>();
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
