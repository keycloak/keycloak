package org.keycloak.testsuite;

import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.page.auth.AuthRealm;
import static org.keycloak.testsuite.page.auth.AuthRealm.TEST;
import org.keycloak.testsuite.page.auth.Login;
import static org.keycloak.testsuite.util.ApiUtil.assignClientRoles;
import static org.keycloak.testsuite.util.ApiUtil.createUser;
import static org.keycloak.testsuite.util.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.util.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.ApiUtil.resetUserPassword;

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
    protected UserRepresentation testUser;

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

        createTestUserWithAdminClient();
//        createTestAdminWithAdminClient();

        // delete all cookies for test realm
        testAuthRealm.navigateTo();
        driver.manage().deleteAllCookies();
    }

    private void createTestUserWithAdminClient() {
        
    }
    
    private void createTestAdminWithAdminClient() {
        testAdmin = new UserRepresentation();
        testAdmin.setUsername("admin");
        testAdmin.setEmail("test@email.test");
        testAdmin.setFirstName("test");
        testAdmin.setLastName("user");
        testAdmin.setEnabled(true);
        
        createUser(testRealmResource, testAdmin);
        testAdmin = findUserByUsername(testRealmResource, testAdmin.getUsername());

        UserResource testAdminResource = testRealmResource.users().get(testAdmin.getId());
        resetUserPassword(testAdminResource, PASSWORD, false);
        
        System.out.println(" adding realm-admin role");
        ClientRepresentation realmManagementClient = findClientByClientId(testRealmResource, "realm-management");
        assignClientRoles(testAdminResource, realmManagementClient.getId(), "realm-admin");
    }

}
