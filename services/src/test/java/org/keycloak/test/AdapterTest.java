package org.keycloak.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.RequiredCredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.models.KeycloakSession;
import org.keycloak.services.models.KeycloakSessionFactory;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.models.UserCredentialModel;
import org.keycloak.services.resources.KeycloakApplication;


import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdapterTest {
    private KeycloakSessionFactory factory;
    private KeycloakSession identitySession;
    private RealmManager adapter;
    private RealmModel realmModel;

    @Before
    public void before() throws Exception {
        factory = KeycloakApplication.buildSessionFactory();
        identitySession = factory.createSession();
        identitySession.getTransaction().begin();
        adapter = new RealmManager(identitySession);
    }

    @After
    public void after() throws Exception {
        identitySession.getTransaction().commit();
        identitySession.close();
        factory.close();
    }

    @Test
    public void installTest() throws Exception {
        new InstallationManager().install(adapter);

    }

    @Test
    public void test1CreateRealm() throws Exception {
        realmModel = adapter.createRealm("JUGGLER");
        realmModel.setAccessCodeLifespan(100);
        realmModel.setCookieLoginAllowed(true);
        realmModel.setEnabled(true);
        realmModel.setName("JUGGLER");
        realmModel.setPrivateKeyPem("0234234");
        realmModel.setPublicKeyPem("0234234");
        realmModel.setTokenLifespan(1000);

        System.out.println(realmModel.getId());
        realmModel = adapter.getRealm(realmModel.getId());
        Assert.assertNotNull(realmModel);
        Assert.assertEquals(realmModel.getAccessCodeLifespan(), 100);
        Assert.assertEquals(realmModel.getTokenLifespan(), 1000);
        Assert.assertEquals(realmModel.isEnabled(), true);
        Assert.assertEquals(realmModel.getName(), "JUGGLER");
        Assert.assertEquals(realmModel.getPrivateKeyPem(), "0234234");
        Assert.assertEquals(realmModel.getPublicKeyPem(), "0234234");
    }

    @Test
    public void test2RequiredCredential() throws Exception {
        test1CreateRealm();
        RequiredCredentialModel creds = new RequiredCredentialModel();
        creds.setSecret(true);
        creds.setType(RequiredCredentialRepresentation.PASSWORD);
        creds.setInput(true);
        realmModel.addRequiredCredential(creds);
        creds = new RequiredCredentialModel();
        creds.setSecret(true);
        creds.setType(RequiredCredentialRepresentation.TOTP);
        creds.setInput(true);
        realmModel.addRequiredCredential(creds);
        List<RequiredCredentialModel> storedCreds = realmModel.getRequiredCredentials();
        Assert.assertEquals(2, storedCreds.size());
        boolean totp = false;
        boolean password = false;
        for (RequiredCredentialModel cred : storedCreds) {
            if (cred.getType().equals(RequiredCredentialRepresentation.PASSWORD)) password = true;
            else if (cred.getType().equals(RequiredCredentialRepresentation.TOTP)) totp = true;
            Assert.assertTrue(cred.isInput());
            Assert.assertTrue(cred.isSecret());
        }
        Assert.assertTrue(totp);
        Assert.assertTrue(password);
    }

    @Test
    public void testCredentialValidation() throws Exception {
        test1CreateRealm();
        UserModel user = realmModel.addUser("bburke");
        UserCredentialModel cred = new UserCredentialModel();
        cred.setType(RequiredCredentialRepresentation.PASSWORD);
        cred.setValue("geheim");
        realmModel.updateCredential(user, cred);
        Assert.assertTrue(realmModel.validatePassword(user, "geheim"));
    }

    @Test
    public void testRoles() throws Exception {
        test1CreateRealm();
        realmModel.addRole("admin");
        realmModel.addRole("user");
        List<RoleModel> roles = realmModel.getRoles();
        Assert.assertEquals(5, roles.size());
        UserModel user = realmModel.addUser("bburke");
        RoleModel role = realmModel.getRole("user");
        realmModel.grantRole(user, role);
        Assert.assertTrue(realmModel.hasRole(user, role));
        System.out.println("Role id: " + role.getId());
        role = realmModel.getRoleById(role.getId());
        Assert.assertNotNull(role);
        Assert.assertEquals("user", role.getName());
    }


}
