package org.keycloak.tests.admin.userstorage;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Assertions;

public class AbstractUserStorageRestTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    protected String createComponent(ComponentRepresentation rep) {
        Response resp = managedRealm.admin().components().add(rep);
        Assertions.assertEquals(201, resp.getStatus());
        resp.close();
        String id = ApiUtil.getCreatedId(resp);

        adminEvents.clear();
        return id;
    }

    protected void removeComponent(String id) {
        managedRealm.admin().components().component(id).remove();
        adminEvents.clear();
    }

    protected ComponentRepresentation createBasicLDAPProviderRep() {
        ComponentRepresentation ldapRep = new ComponentRepresentation();
        ldapRep.setName("ldap2");
        ldapRep.setProviderId("ldap");
        ldapRep.setProviderType(UserStorageProvider.class.getName());
        ldapRep.setConfig(new MultivaluedHashMap<>());
        ldapRep.getConfig().putSingle("priority", Integer.toString(2));
        ldapRep.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.name());
        return ldapRep;
    }
}
