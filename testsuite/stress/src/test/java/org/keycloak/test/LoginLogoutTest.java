package org.keycloak.test;

import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.test.stress.MaxRateExecutor;
import org.keycloak.test.stress.StressExecutor;
import org.keycloak.test.stress.TestFactory;
import org.keycloak.test.stress.tests.LoginLogout;
import org.keycloak.testsuite.adapter.AdapterTestStrategy;
import org.keycloak.testsuite.adapter.CallAuthenticatedServlet;
import org.keycloak.testsuite.adapter.CustomerDatabaseServlet;
import org.keycloak.testsuite.adapter.CustomerServlet;
import org.keycloak.testsuite.adapter.InputServlet;
import org.keycloak.testsuite.adapter.ProductServlet;
import org.keycloak.testsuite.adapter.SessionServlet;
import org.keycloak.testsuite.rule.AbstractKeycloakRule;

import java.net.URL;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class LoginLogoutTest {
    @ClassRule
    public static AbstractKeycloakRule keycloakRule = new AbstractKeycloakRule() {
        @Override
        protected void configure(KeycloakSession session, RealmManager manager, RealmModel adminRealm) {
            RealmModel realm = AdapterTestStrategy.baseAdapterTestInitialization(session, manager, adminRealm, getClass());

            URL url = getClass().getResource("/adapter-test/cust-app-keycloak.json");
            createApplicationDeployment()
                    .name("customer-portal").contextPath("/customer-portal")
                    .servletClass(org.keycloak.test.CustomerDatabaseServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();

        }
    };

    @Test
    public void testStressExecutor() throws Exception {
        System.out.println("*************************");
        System.out.println();
        System.out.println();
        StressExecutor executor = new StressExecutor();
        LoginLogout test = new LoginLogout();
        test.authServerUrl("http://localhost:8081/auth")
                .realm("demo")
                .username("bburke@redhat.com")
                .password("password")
                .securedResourceUrl("http://localhost:8081/customer-portal");
        test.init();
        executor.addTest(test, 5);
        long time = executor.execute();
        System.out.println("Took: " + time );
    }

    /*
**************************
*   Bill's Best Result   *
**************************
Threads: 13
Total Time: 1018
Successes: 400
Iterations: 400
Average time: 32.8075
Rate: 0.030480835174883793
     */

    @Test
    public void testRate() throws Exception {
        System.out.println("*************************");
        System.out.println();
        System.out.println();
        TestFactory factory = new TestFactory() {
            @Override
            public org.keycloak.test.stress.Test create() {
                LoginLogout test = new LoginLogout();
                test.authServerUrl("http://localhost:8081/auth")
                        .realm("demo")
                        .username("bburke@redhat.com")
                        .password("password")
                        .securedResourceUrl("http://localhost:8081/customer-portal");
                return test;
            }
        };
        MaxRateExecutor executor = new MaxRateExecutor();
        executor.best(factory, 10);
        executor.printResults();
        executor.printSummary();
    }

}
