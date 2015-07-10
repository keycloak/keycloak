package org.keycloak.testsuite.adapter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.keycloak.testsuite.page.adapter.JSConsoleExample;
import static org.keycloak.testsuite.util.RealmAssert.assertCurrentUrlStartsWithLoginUrlOf;
import static org.keycloak.testsuite.util.SeleniumUtils.pause;

public abstract class AbstractJSConsoleExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private JSConsoleExample jsConsoleExample;

    @Deployment(name = JSConsoleExample.DEPLOYMENT_NAME)
    private static WebArchive jsConsoleExample() throws IOException {
        return exampleDeployment("js-console");
    }

    @Override
    public void loadAdapterTestRealmsTo(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm(new File(EXAMPLES_HOME_DIR + "/js-console/example-realm.json")));
    }

    @Override
    public void setPageUriTemplateValues() {
        super.setPageUriTemplateValues();
        testRealm.setTemplateValues("example");
    }

    @Test
    @Ignore("Need to put deployment's real context path into test realm. It is not /js-console.")
    public void testJSConsoleAuth() {
        jsConsoleExample.navigateTo();
        assertCurrentUrlStartsWith(jsConsoleExample);

        pause(1000);

        jsConsoleExample.logIn();
        loginPage.login("user", "invalid-password");
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        loginPage.login("invalid-user", "password");
        assertCurrentUrlStartsWithLoginUrlOf(testRealm);

        loginPage.login("user", "password");
        assertCurrentUrlStartsWith(jsConsoleExample);
        assertTrue(driver.getPageSource().contains("Init Success (Authenticated)"));

        pause(1000);

        jsConsoleExample.logOut();
        assertCurrentUrlStartsWith(jsConsoleExample);
        assertTrue(driver.getPageSource().contains("Init Success (Not Authenticated)"));
    }

}
