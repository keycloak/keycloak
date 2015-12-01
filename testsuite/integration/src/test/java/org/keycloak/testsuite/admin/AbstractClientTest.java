package org.keycloak.testsuite.admin;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientTest {

    protected static final String REALM_NAME = "admin-client-test";

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    protected Keycloak keycloak;
    protected RealmResource realm;

    @Before
    public void before() {
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                RealmModel testRealm = manager.createRealm(REALM_NAME);
                testRealm.setEnabled(true);
                testRealm.setAccessCodeLifespanUserAction(600);
                KeycloakModelUtils.generateRealmKeys(testRealm);

                appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
            }
        });

        keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID);
        realm = keycloak.realm(REALM_NAME);
    }

    @After
    public void after() {
        keycloak.close();

        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                RealmModel realm = manager.getRealmByName(REALM_NAME);
                if (realm != null) {
                    manager.removeRealm(realm);
                }
            }
        });
    }

    public static <T> void assertNames(List<T> actual, String... expected) {
        Arrays.sort(expected);
        String[] actualNames = names(actual);
        assertArrayEquals("Expected: " + Arrays.toString(expected) + ", was: " + Arrays.toString(actualNames), expected, actualNames);
    }

    public static <T> List<T> sort(List<T> list) {
        Collections.sort(list, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return name(o1).compareTo(name(o2));
            }
        });
        return list;
    }

    public static <T> String[] names(List<T> list) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = name(list.get(i));
        }
        Arrays.sort(names);
        return names;
    }

    public static String name(Object o1) {
        if (o1 instanceof RealmRepresentation) {
            return ((RealmRepresentation) o1).getRealm();
        } else if (o1 instanceof ClientRepresentation) {
            return ((ClientRepresentation) o1).getClientId();
        } else if (o1 instanceof IdentityProviderRepresentation) {
            return ((IdentityProviderRepresentation) o1).getAlias();
        }
        throw new IllegalArgumentException();
    }

}
