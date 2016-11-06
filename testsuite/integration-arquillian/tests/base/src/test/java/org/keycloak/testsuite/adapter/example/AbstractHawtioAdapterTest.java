package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.HawtioPage;

import java.io.File;
import java.util.List;

import static org.keycloak.testsuite.auth.page.AuthRealm.DEMO;
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

/**
 * @author mhajas
 */
public class AbstractHawtioAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private HawtioPage hawtioPage;

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm("/adapter-test/hawtio-realm/demorealm.json"));
    }

    @Test
    @Ignore //Waiting for PATCH-1446
    public void hawtioTest() {
        testRealmLoginPage.setAuthRealm(DEMO);
        hawtioPage.navigateTo();
        assertCurrentUrlStartsWith(testRealmLoginPage);
        testRealmLoginPage.form().login("root", "password");

        assertCurrentUrlStartsWith(hawtioPage.getDriver(), hawtioPage.toString() + "/welcome");

    }
}
