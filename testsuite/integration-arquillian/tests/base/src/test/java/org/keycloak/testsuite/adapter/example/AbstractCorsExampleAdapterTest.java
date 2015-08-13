package org.keycloak.testsuite.adapter.example;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import org.keycloak.testsuite.adapter.page.AngularCorsProductExample;
import org.keycloak.testsuite.adapter.page.CorsDatabaseServiceExample;
import org.keycloak.testsuite.auth.page.account.Account;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;

/**
 * Created by fkiss.
 */
public class AbstractCorsExampleAdapterTest extends AbstractExampleAdapterTest {

    public static final String CORS = "cors";

    @Page
    private AngularCorsProductExample angularCorsProductExample;

    @Page
    private Account testRealmAccount;

    @Deployment(name = AngularCorsProductExample.DEPLOYMENT_NAME)
    private static WebArchive angularCorsProductExample() throws IOException {
        return exampleDeployment(AngularCorsProductExample.DEPLOYMENT_NAME, "angular-cors-product");
    }

    @Deployment(name = CorsDatabaseServiceExample.DEPLOYMENT_NAME)
    private static WebArchive corsDatabaseServiceExample() throws IOException {
        return exampleDeployment(CorsDatabaseServiceExample.DEPLOYMENT_NAME);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(
                loadRealm(new File(EXAMPLES_HOME_DIR + "/cors/cors-realm.json")));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealm.setAuthRealm(CORS);
        testRealmLogin.setAuthRealm(CORS);
        testRealmAccount.setAuthRealm(CORS);
    }

    @Before
    public void beforeDemoExampleTest() {
        angularCorsProductExample.navigateTo();
        driver.manage().deleteAllCookies();
    }

    @Test
    public void angularCorsProductTest() {
        angularCorsProductExample.navigateTo();
        testRealmLogin.form().login("bburke@redhat.com", "password");

        assertCurrentUrlStartsWith(angularCorsProductExample);
    }
}