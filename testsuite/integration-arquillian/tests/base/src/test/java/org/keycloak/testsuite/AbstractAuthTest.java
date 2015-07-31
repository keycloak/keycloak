package org.keycloak.testsuite;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.auth.AuthRealm;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;
import org.keycloak.testsuite.page.auth.Login;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAuthTest extends AbstractKeycloakTest {

    @Page
    protected AuthRealm testAuthRealm;
    @Page
    protected Login testLogin;

    protected RealmResource testRealmResource;
    protected UserRepresentation testAdmin;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setRealm(TEST);
        testRealmRep.setEnabled(true);
        testRealms.add(testRealmRep);
    }

    @Before
    public void beforeAuthTest() {
        testRealmResource = keycloak.realm(TEST);

        createTestUser();

        testAuthRealm.navigateTo();
        driver.manage().deleteAllCookies();
    }

    public void createTestUser() {
        System.out.println("creating test user");

        testAdmin = new UserRepresentation();
        testAdmin.setUsername("admin");
        testAdmin.setEmail("test@email.test");
        testAdmin.setFirstName("test");
        testAdmin.setLastName("user");
        testAdmin.setEnabled(true);
        Response response = testRealmResource.users().create(testAdmin);
        response.close();

        testAdmin = findUserByUsername(testRealmResource, testAdmin.getUsername());

        System.out.println(" resetting password");

        UserResource testUserResource = testRealmResource.users().get(testAdmin.getId());
        CredentialRepresentation testUserPassword = new CredentialRepresentation();
        testUserPassword.setType(PASSWORD);
        testUserPassword.setValue(PASSWORD);
        testUserPassword.setTemporary(false);
        testUserResource.resetPassword(testUserPassword);

        System.out.println(" adding realm-admin role");

        ClientRepresentation realmManagementClient = findClientByClientId(testRealmResource, "realm-management");
        RoleScopeResource rsr = testUserResource.roles().clientLevel(realmManagementClient.getId());

        List<RoleRepresentation> realmMgmtRoles = new ArrayList<>();
        for (RoleRepresentation rr : rsr.listAvailable()) {
            if ("realm-admin".equals(rr.getName())) {
                realmMgmtRoles.add(rr);
            }
        }
        rsr.add(realmMgmtRoles);
    }

}
