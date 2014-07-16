package org.keycloak.testsuite.admin;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminClientTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    private Keycloak keycloak;
    private RealmRepresentation realmRep;

    @Before
    public void before() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                RealmManager realmManager = new RealmManager(session);

                realmRep = new RealmRepresentation();
                realmRep.setRealm("admin-client-test");
                realmRep.setEnabled(true);
                realmRep.setSslNotRequired(true);
                realmRep.setPasswordCredentialGrantAllowed(true);

                RealmModel realm = realmManager.importRealm(realmRep);

                UserModel admin = session.users().addUser(realm, "admin");
                admin.setEnabled(true);
                admin.grantRole(realm.getApplicationByName("realm-management").getRole(AdminRoles.REALM_ADMIN));

                UserCredentialModel cred = new UserCredentialModel();
                cred.setType(UserCredentialModel.PASSWORD);
                cred.setValue("admin");
                admin.updateCredential(cred);
            }
        });

        keycloak = Keycloak.getInstance("http://localhost:8081/auth", "admin-client-test", "admin", "admin", Constants.ADMIN_CONSOLE_APPLICATION);
    }

    @After
    public void after() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                manager.removeRealm(manager.getRealmByName("admin-client-test"));
            }
        });
    }

    @Test
    public void getRealm() {
        RealmRepresentation rep = keycloak.realm().getRepresentation();
        assertEquals("admin-client-test", rep.getRealm());
        // TODO Check rep
    }

    @Test
    public void addUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");

        keycloak.services().userService().create(user);

        try {
            keycloak.services().userService().create(user);
            fail("Expected failure");
        } catch (KeycloakException e) {
        }
    }

}
