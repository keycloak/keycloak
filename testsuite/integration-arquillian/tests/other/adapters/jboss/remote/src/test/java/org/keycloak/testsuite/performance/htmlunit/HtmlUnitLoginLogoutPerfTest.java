package org.keycloak.testsuite.performance.htmlunit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.performance.LoginLogoutTestParameters;
import org.keycloak.testsuite.performance.OperationTimeoutException;
import org.keycloak.testsuite.performance.PerformanceMeasurement;
import org.keycloak.testsuite.performance.PerformanceTest;
import org.keycloak.testsuite.performance.page.AppProfileJEE;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.performance.LoginLogoutTestParameters.ACCESS_REQUEST_TIME;
import static org.keycloak.testsuite.performance.LoginLogoutTestParameters.LOGIN_REQUEST_TIME;
import static org.keycloak.testsuite.performance.LoginLogoutTestParameters.LOGIN_VERIFY_REQUEST_TIME;
import static org.keycloak.testsuite.performance.LoginLogoutTestParameters.LOGOUT_REQUEST_TIME;
import static org.keycloak.testsuite.performance.LoginLogoutTestParameters.LOGOUT_VERIFY_REQUEST_TIME;
import static org.keycloak.testsuite.performance.LoginLogoutTestParameters.PASSWORD_HASH_ITERATIONS;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

/**
 *
 * @author tkyjovsk
 */
@AppServerContainer("app-server-remote")
public class HtmlUnitLoginLogoutPerfTest extends HtmlUnitPerformanceTest {

    private static final Logger LOG = Logger.getLogger(HtmlUnitLoginLogoutPerfTest.class);

    private static final String EXAMPLES = "Examples";

    private String unsecuredUrl;
    private String securedUrl;
    private String username;
    private String password;
    private String loginPageUrl;

    @Page
    protected AppProfileJEE appProfileJEEPage;

    protected static WebArchive warDeployment(String filename) throws IOException {
        return ShrinkWrap.createFromZipFile(WebArchive.class,
                new File(EXAMPLES_HOME + "/" + filename + ".war"))
                .addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);
    }

    @Deployment(name = AppProfileJEE.DEPLOYMENT_NAME)
    private static WebArchive appProfileJEE() throws IOException {
        return warDeployment("keycloak-quickstart-app-profile-jee-0.5-SNAPSHOT");
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(EXAMPLES);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation examplesRealm = loadRealm("/examples-realm.json");
        examplesRealm.setPasswordPolicy("hashIterations(" + PASSWORD_HASH_ITERATIONS + ")");
        testRealms.add(examplesRealm);
    }

    @Before
    public void beforeLoginLogoutTest() {
        unsecuredUrl = appProfileJEEPage + "/index.jsp";
        securedUrl = appProfileJEEPage + "/profile.jsp";
        username = "secure-user";
        password = "password";
        loginPageUrl = testRealmLoginPage.toString();
    }

    @Override
    public PerformanceTest.Runnable newRunnable() {
        return new Runnable();
    }

    @Override
    protected boolean isMeasurementWithinLimits(PerformanceMeasurement measurement) {
        return LoginLogoutTestParameters.isMeasurementWithinLimits(measurement);
    }

    public class Runnable extends HtmlUnitPerformanceTest.Runnable {

        @Override
        public void performanceScenario() throws Exception {
            LOG.trace(String.format("Starting login-logout scenario #%s", getLoopCounter()));
            driver.manage().deleteAllCookies();

            // ACCESS
            LOG.trace(String.format("Accessing secured URL: %s", securedUrl));
            try {
                timer.reset();
                driver.get(securedUrl);
                assertTrue(driver.getCurrentUrl().startsWith(loginPageUrl));
            } catch (TimeoutException ex) {
                throw new OperationTimeoutException(ACCESS_REQUEST_TIME, ex);
            }
            statistics.addValue(ACCESS_REQUEST_TIME, timer.getElapsedTime());

            // LOGIN
            LOG.trace("Logging in");
            try {
                driver.findElement(By.id("username")).clear();
                driver.findElement(By.id("username")).sendKeys(username);
                driver.findElement(By.id("password")).clear();
                driver.findElement(By.id("password")).sendKeys(password);
                timer.reset();
                driver.findElement(By.name("login")).click();
                assertTrue(driver.getCurrentUrl().equals(securedUrl));
            } catch (TimeoutException ex) {
                throw new OperationTimeoutException(LOGIN_REQUEST_TIME, ex);
            }
            statistics.addValue(LOGIN_REQUEST_TIME, timer.getElapsedTime());

            // VERIFY LOGIN
            LOG.trace("Verifying login");
            try {
                timer.reset();
                driver.get(securedUrl);
                assertTrue(driver.getCurrentUrl().equals(securedUrl));
            } catch (TimeoutException ex) {
                throw new OperationTimeoutException(LOGIN_VERIFY_REQUEST_TIME, ex);
            }
            statistics.addValue(LOGIN_VERIFY_REQUEST_TIME, timer.getElapsedTime());

            // LOGOUT
            LOG.trace("Logging out");
            try {
                timer.reset();
                driver.findElement(By.xpath("//button[text()='Logout']")).click();
                assertTrue(driver.getCurrentUrl().startsWith(unsecuredUrl));
            } catch (TimeoutException ex) {
                throw new OperationTimeoutException(LOGOUT_REQUEST_TIME, ex);
            }
            statistics.addValue(LOGOUT_REQUEST_TIME, timer.getElapsedTime());

            // VERIFY LOGOUT
            LOG.trace("Verifying logout");
            try {
                timer.reset();
                driver.get(securedUrl);
                assertTrue(driver.getCurrentUrl().startsWith(loginPageUrl));
            } catch (TimeoutException ex) {
                throw new OperationTimeoutException(LOGOUT_VERIFY_REQUEST_TIME, ex);
            }
            statistics.addValue(LOGOUT_VERIFY_REQUEST_TIME, timer.getElapsedTime());

            LOG.trace("Logged out");
        }

    }

}
