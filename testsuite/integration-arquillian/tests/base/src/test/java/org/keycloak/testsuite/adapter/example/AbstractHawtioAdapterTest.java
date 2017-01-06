package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.HawtioPage;

import java.util.List;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * @author mhajas
 */
public abstract class AbstractHawtioAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private HawtioPage hawtioPage;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/hawtio-realm/demorealm.json"));
    }

    @Test
    public void hawtioLoginAndLogoutTest() {
        testRealmLoginPage.setAuthRealm(DEMO);

        hawtioPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login("root", "password");

        assertCurrentUrlStartsWith(hawtioPage.getDriver(), hawtioPage.toString() + "/welcome");

        hawtioPage.logout();
        assertCurrentUrlStartsWith(testRealmLoginPage);

        hawtioPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmLoginPage);
    }
}
