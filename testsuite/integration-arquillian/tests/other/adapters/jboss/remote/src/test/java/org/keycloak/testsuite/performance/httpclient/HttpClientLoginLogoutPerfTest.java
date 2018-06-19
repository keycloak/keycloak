package org.keycloak.testsuite.performance.httpclient;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
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

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
public class HttpClientLoginLogoutPerfTest extends HttpClientPerformanceTest {

    private static final Logger LOG = Logger.getLogger(HttpClientLoginLogoutPerfTest.class);

    private static final String TEST_REALM = "Test";

    private String securedUrl;
    private String logoutUrl;
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
        return exampleDeployment("keycloak-test-app-profile-jee");
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(TEST_REALM);
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation examplesRealm = loadRealm("/test-realm.json");
        examplesRealm.setPasswordPolicy("hashIterations(" + PASSWORD_HASH_ITERATIONS + ")");
        testRealms.add(examplesRealm);
    }

    @Before
    public void beforeLoginLogoutTest() {
        securedUrl = appProfileJEEPage + "/profile.jsp";
        logoutUrl = appProfileJEEPage + "/index.jsp?logout=true";
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

    public class Runnable extends HttpClientPerformanceTest.Runnable {

        @Override
        public void performanceScenario() throws IOException, OperationTimeoutException {
            LOG.trace(String.format("Starting login-logout scenario #%s", getLoopCounter()));
            context.getCookieStore().clear();

            // ACCESS
            String pageContent;
            final HttpGet getSecuredPageRequest = new HttpGet(securedUrl);
            LOG.trace(String.format("Accessing secured URL: %s", getSecuredPageRequest));
            LOG.trace(getSecuredPageRequest);
            timer.reset();
            try (CloseableHttpResponse r = client.execute(getSecuredPageRequest, context)) {
                assertEquals("ACCESS_REQUEST OK", HTTP_OK, r.getStatusLine().getStatusCode());
                logRedirects();
                assertEquals("ACCESS_REQUEST has 1 redirect", 1, context.getRedirectLocations().size());
                assertTrue("ACCESS_REQUEST redirects to login page", getLastRedirect().toASCIIString().startsWith(loginPageUrl));
                pageContent = EntityUtils.toString(r.getEntity());
            } catch (SocketException ex) {
                throw new OperationTimeoutException(ACCESS_REQUEST_TIME, ex);
            } catch (SocketTimeoutException ex) {
                throw new OperationTimeoutException(ACCESS_REQUEST_TIME, ex.bytesTransferred, ex);
            }
            statistics.addValue(ACCESS_REQUEST_TIME, timer.getElapsedTime());

            // LOGIN
            final HttpPost loginRequest = new HttpPost(getLoginUrlFromPage(pageContent));
            List<NameValuePair> credentials = new ArrayList<>();
            credentials.add(new BasicNameValuePair("username", username));
            credentials.add(new BasicNameValuePair("password", password));
            loginRequest.setEntity(new UrlEncodedFormEntity(credentials));

            LOG.trace("Logging in");
            LOG.trace(loginRequest);
            timer.reset();
            try (CloseableHttpResponse r = client.execute(loginRequest, context)) {
                assertEquals("LOGIN_REQUEST OK", HTTP_OK, r.getStatusLine().getStatusCode());
                logRedirects();
                assertEquals("LOGIN_REQUEST has 2 redirects", 2, context.getRedirectLocations().size());
                assertTrue("LOGIN_REQUEST redirects to secured page", getLastRedirect().toASCIIString().equals(securedUrl));
            } catch (SocketException ex) {
                throw new OperationTimeoutException(LOGIN_REQUEST_TIME, ex);
            } catch (SocketTimeoutException ex) {
                throw new OperationTimeoutException(LOGIN_REQUEST_TIME, ex.bytesTransferred, ex);
            }
            statistics.addValue(LOGIN_REQUEST_TIME, timer.getElapsedTime());

            // VERIFY LOGIN
            LOG.trace("Verifying login");
            LOG.trace(getSecuredPageRequest);
            timer.reset();
            try (CloseableHttpResponse r = client.execute(getSecuredPageRequest, context)) {
                assertEquals("LOGIN_VERIFY_REQUEST OK", HTTP_OK, r.getStatusLine().getStatusCode());
                logRedirects();
                assertEquals("LOGIN_VERIFY_REQUEST has 0 redirects", 0, context.getRedirectLocations().size());
            } catch (SocketException ex) {
                throw new OperationTimeoutException(LOGIN_VERIFY_REQUEST_TIME, ex);
            } catch (SocketTimeoutException ex) {
                throw new OperationTimeoutException(LOGIN_VERIFY_REQUEST_TIME, ex.bytesTransferred, ex);
            }
            statistics.addValue(LOGIN_VERIFY_REQUEST_TIME, timer.getElapsedTime());

            // LOGOUT
            final HttpGet logoutRequest = new HttpGet(logoutUrl);
            LOG.trace("Logging out");
            LOG.trace(logoutRequest);
            timer.reset();
            try (CloseableHttpResponse r = client.execute(logoutRequest, context)) {
                assertEquals("LOGOUT_REQUEST OK", HTTP_OK, r.getStatusLine().getStatusCode());
                logRedirects();
                assertEquals("LOGOUT_REQUEST has 0 redirects", 0, context.getRedirectLocations().size());
            } catch (SocketException ex) {
                throw new OperationTimeoutException(LOGOUT_REQUEST_TIME, ex);
            } catch (SocketTimeoutException ex) {
                throw new OperationTimeoutException(LOGOUT_REQUEST_TIME, ex.bytesTransferred, ex);
            }
            statistics.addValue(LOGOUT_REQUEST_TIME, timer.getElapsedTime());

            // VERIFY LOGOUT
            LOG.trace("Verifying logout");
            LOG.trace(getSecuredPageRequest);
            timer.reset();
            try (CloseableHttpResponse r = client.execute(getSecuredPageRequest, context)) {
                assertEquals("LOGOUT_VERIFY_REQUEST OK", HTTP_OK, r.getStatusLine().getStatusCode());
                logRedirects();
                assertEquals("LOGOUT_VERIFY_REQUEST has 1 redirect", 1, context.getRedirectLocations().size());
                assertTrue("LOGOUT_VERIFY_REQUEST redirects to login page", getLastRedirect().toASCIIString().startsWith(loginPageUrl));
            } catch (SocketException ex) {
                throw new OperationTimeoutException(LOGOUT_VERIFY_REQUEST_TIME, ex);
            } catch (SocketTimeoutException ex) {
                throw new OperationTimeoutException(LOGOUT_VERIFY_REQUEST_TIME, ex.bytesTransferred, ex);
            }
            statistics.addValue(LOGOUT_VERIFY_REQUEST_TIME, timer.getElapsedTime());

            LOG.trace("Logged out");

        }

        private URI getLastRedirect() {
            return context.getRedirectLocations().get(context.getRedirectLocations().size() - 1);
        }

        private void logRedirects() {
            int i = 0;
            for (URI uri : context.getRedirectLocations()) {
                LOG.trace(String.format("--> REDIRECT %s: %s", ++i, uri.toASCIIString()));
            }
        }

        public String getRedirectLocation(CloseableHttpResponse r) {
            Header locationHeader = r.getFirstHeader("Location");
            assertNotNull(locationHeader);
            return locationHeader.getValue();
        }

        private String getLoginUrlFromPage(String content) {
            String formActionRegex = "<form[^>]+action\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
            Pattern p = Pattern.compile(formActionRegex);
            Matcher m = p.matcher(content);
            if (m.find()) {
                return m.group(1);
            } else {
                throw new IllegalStateException("Login url counldn't be parsed form page.");
            }
        }

    }

}
