package org.keycloak.testsuite.cookies;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpCoreContext;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.AdminRoles;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.Cookie;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST;

import org.junit.After;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class CookiesPathTest extends AbstractKeycloakTest {

    @Page
    protected LoginPage loginPage;

    public static final String AUTH_SESSION_VALUE = "1869c345-2f90-4724-936d-a1a1ef41dea7";

    public static final String AUTH_SESSION_VALUE_NODE = "1869c345-2f90-4724-936d-a1a1ef41dea7.host";

    public static final String OLD_COOKIE_PATH = "/auth/realms/foo";

    public static final String KC_RESTART = "KC_RESTART";

    private CloseableHttpClient httpClient = null;

    private static final List<String> KEYCLOAK_COOKIE_NAMES = Arrays.asList("KC_RESTART", "AUTH_SESSION_ID", "KEYCLOAK_IDENTITY", "KEYCLOAK_SESSION");

    @After
    public void closeHttpClient() throws IOException {
        if (httpClient != null) httpClient.close();
    }

    @Test
    public void testCookiesPath() {
        // navigate to "/realms/foo/account" and them remove cookies in the browser for the current path
        // first access to the path means there are no cookies being sent
        // we are redirected to login page and Keycloak sets cookie's path to "/auth/realms/foo/"
        URLUtils.navigateToUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/foo/account");
        driver.manage().deleteAllCookies();

        Assert.assertTrue("There shouldn't be any cookies sent!", driver.manage().getCookies().isEmpty());

        // refresh the page and cookies are sent within the request
        driver.navigate().refresh();

        Set<Cookie> cookies = driver.manage().getCookies();
        Assert.assertTrue("There should be cookies sent!", cookies.size() > 0);
        // check cookie's path, for some reason IE adds extra slash to the beginning of the path
        cookies.stream()
                .filter(cookie -> KEYCLOAK_COOKIE_NAMES.contains(cookie.getName()))
                .forEach(cookie -> Assert.assertThat(cookie.getPath(), Matchers.endsWith("/auth/realms/foo/")));

        // now navigate to realm which name overlaps the first realm and delete cookies for that realm (foobar)
        URLUtils.navigateToUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/foobar/account");
        driver.manage().deleteAllCookies();

        // cookies shouldn't be sent for the first access to /realms/foobar/account
        // At this moment IE would sent cookies for /auth/realms/foo without the fix
        cookies = driver.manage().getCookies();
        Assert.assertTrue("There shouldn't be any cookies sent!", cookies.isEmpty());

        // navigate to account and check if correct cookies were sent
        URLUtils.navigateToUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/foobar/account");
        cookies = driver.manage().getCookies();

        Assert.assertTrue("There should be cookies sent!", cookies.size() > 0);
        // check cookie's path, for some reason IE adds extra slash to the beginning of the path
        cookies.stream()
                .filter(cookie -> KEYCLOAK_COOKIE_NAMES.contains(cookie.getName()))
                .forEach(cookie -> Assert.assertThat(cookie.getPath(), Matchers.endsWith("/auth/realms/foobar/")));

        // lets back to "/realms/foo/account" to test the cookies for "foo" realm are still there and haven't been (correctly) sent to "foobar"
        URLUtils.navigateToUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/foo/account");

        cookies = driver.manage().getCookies();
        Assert.assertTrue("There should be cookies sent!", cookies.size() > 0);
        cookies.stream()
                .filter(cookie -> KEYCLOAK_COOKIE_NAMES.contains(cookie.getName()))
                .forEach(cookie -> Assert.assertThat(cookie.getPath(), Matchers.endsWith("/auth/realms/foo/")));
    }

    @Test
    public void testMultipleCookies() throws IOException {
        String requestURI = OAuthClient.AUTH_SERVER_ROOT + "/realms/foo/account";
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // create old cookie with wrong path
        BasicClientCookie wrongCookie = new BasicClientCookie(AuthenticationSessionManager.AUTH_SESSION_ID, AUTH_SESSION_VALUE);
        wrongCookie.setDomain(AUTH_SERVER_HOST);
        wrongCookie.setPath(OLD_COOKIE_PATH);
        wrongCookie.setExpiryDate(calendar.getTime());

        // obtain new cookies
        CookieStore cookieStore = getCorrectCookies(requestURI);
        cookieStore.addCookie(wrongCookie);

        Assert.assertThat(cookieStore.getCookies(), Matchers.hasSize(3));

        login(requestURI, cookieStore);

        // old cookie has been removed
        // now we have AUTH_SESSION_ID, KEYCLOAK_IDENTITY, KEYCLOAK_SESSION
        Assert.assertThat(cookieStore.getCookies().stream().map(org.apache.http.cookie.Cookie::getName).collect(Collectors.toList()), 
                Matchers.hasItems("AUTH_SESSION_ID", "KEYCLOAK_IDENTITY", "KEYCLOAK_SESSION"));

        // does each cookie's path end with "/"
        cookieStore.getCookies().stream().filter(c -> !"OAuth_Token_Request_State".equals(c.getName())).map(org.apache.http.cookie.Cookie::getPath).forEach(path ->Assert.assertThat(path, Matchers.endsWith("/")));

        // KEYCLOAK_SESSION should end by AUTH_SESSION_ID value
        String authSessionId = cookieStore.getCookies().stream().filter(c -> "AUTH_SESSION_ID".equals(c.getName())).findFirst().get().getValue();
        String KCSessionId = cookieStore.getCookies().stream().filter(c -> "KEYCLOAK_SESSION".equals(c.getName())).findFirst().get().getValue();
        String KCSessionSuffix = KCSessionId.split("/")[2];
        Assert.assertThat(authSessionId, Matchers.containsString(KCSessionSuffix));
    }

    @Test
    public void testOldCookieWithWrongPath() {
        ContainerAssume.assumeAuthServerSSL();

        Cookie wrongCookie = new Cookie(AuthenticationSessionManager.AUTH_SESSION_ID, AUTH_SESSION_VALUE,
                null, OLD_COOKIE_PATH, null, false, true);

        URLUtils.navigateToUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/foo/account");
        driver.manage().deleteAllCookies();

        // add old cookie with wrong path
        driver.manage().addCookie(wrongCookie);
        Set<Cookie> cookies = driver.manage().getCookies();
        Assert.assertThat(cookies, Matchers.hasSize(1));

        oauth.realm("foo").redirectUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/foo/account").clientId("account").openLoginForm();

        loginPage.login("foo", "password");

        // old cookie has been removed and new cookies have been added
        cookies = driver.manage().getCookies().stream()
                .filter(cookie -> KEYCLOAK_COOKIE_NAMES.contains(cookie.getName()))
                .collect(Collectors.toSet());
        Assert.assertThat(cookies, Matchers.hasSize(3));

        // does each cookie's path end with "/"
        cookies.stream().map(Cookie::getPath).forEach(path -> Assert.assertThat(path, Matchers.endsWith("/")));

        // KEYCLOAK_SESSION should end by AUTH_SESSION_ID value
        String authSessionId = cookies.stream().filter(c -> "AUTH_SESSION_ID".equals(c.getName())).findFirst().get().getValue();
        String KCSessionId = cookies.stream().filter(c -> "KEYCLOAK_SESSION".equals(c.getName())).findFirst().get().getValue();
        String KCSessionSuffix = KCSessionId.split("/")[2];
        Assert.assertThat(authSessionId, Matchers.containsString(KCSessionSuffix));
    }

    @Test
    public void testOldCookieWithNodeInValue() throws IOException {
        String requestURI = OAuthClient.AUTH_SERVER_ROOT + "/realms/foo/account";
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        // create old cookie with wrong path
        BasicClientCookie wrongCookie = new BasicClientCookie(AuthenticationSessionManager.AUTH_SESSION_ID, AUTH_SESSION_VALUE_NODE);
        wrongCookie.setDomain(AUTH_SERVER_HOST);
        wrongCookie.setPath(OLD_COOKIE_PATH);
        wrongCookie.setExpiryDate(calendar.getTime());

        // obtain new cookies
        CookieStore cookieStore = getCorrectCookies(requestURI);
        cookieStore.addCookie(wrongCookie);

        Assert.assertThat(cookieStore.getCookies(), Matchers.hasSize(3));

        login(requestURI, cookieStore);

        // old cookie has been removed
        // now we have AUTH_SESSION_ID, KEYCLOAK_IDENTITY, KEYCLOAK_SESSION, OAuth_Token_Request_State
        Assert.assertThat(cookieStore.getCookies().stream().map(org.apache.http.cookie.Cookie::getName).collect(Collectors.toList()), 
                Matchers.hasItems("AUTH_SESSION_ID", "KEYCLOAK_IDENTITY", "KEYCLOAK_SESSION"));

        // does each cookie's path end with "/"
        cookieStore.getCookies().stream().filter(c -> !"OAuth_Token_Request_State".equals(c.getName())).map(org.apache.http.cookie.Cookie::getPath).forEach(path ->Assert.assertThat(path, Matchers.endsWith("/")));

        // KEYCLOAK_SESSION should end by AUTH_SESSION_ID value
        String authSessionId = cookieStore.getCookies().stream().filter(c -> "AUTH_SESSION_ID".equals(c.getName())).findFirst().get().getValue();
        String KCSessionId = cookieStore.getCookies().stream().filter(c -> "KEYCLOAK_SESSION".equals(c.getName())).findFirst().get().getValue();
        String KCSessionSuffix = KCSessionId.split("/")[2];
        Assert.assertThat(authSessionId, Matchers.containsString(KCSessionSuffix));
    }

    /**
     * Add two realms which names are overlapping i.e foo and foobar
     * @param testRealms
     */
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder foo = RealmBuilder.create().name("foo");
        foo.user(UserBuilder.create().username("foo").password("password").role("account", AdminRoles.ADMIN)
                .role("account", AccountRoles.MANAGE_ACCOUNT).role("account", AccountRoles.VIEW_PROFILE).role("account", AccountRoles.MANAGE_ACCOUNT_LINKS));
        testRealms.add(foo.build());

        RealmBuilder foobar = RealmBuilder.create().name("foobar");
        foo.user(UserBuilder.create().username("foobar").password("password").role("account", AdminRoles.ADMIN)
                .role("account", AccountRoles.MANAGE_ACCOUNT).role("account", AccountRoles.VIEW_PROFILE).role("account", AccountRoles.MANAGE_ACCOUNT_LINKS));
        testRealms.add(foobar.build());
    }

    // if the client is closed before the response is read, it throws 
    // org.apache.http.ConnectionClosedException: Premature end of Content-Length delimited message body
    // that's why the this.httpClient is introduced, the client is closed either here or after test method
    private CloseableHttpResponse sendRequest(HttpRequestBase request, CookieStore cookieStore, HttpCoreContext localContext) throws IOException {
        if (httpClient != null) httpClient.close();
        httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).setRedirectStrategy(new LaxRedirectStrategy()).build();
        return httpClient.execute(request, localContext);
    }

    private CookieStore getCorrectCookies(String uri) throws IOException {
        CookieStore cookieStore = new BasicCookieStore();

        HttpGet request = new HttpGet(uri);
        try (CloseableHttpResponse response = sendRequest(request, new BasicCookieStore(), new HttpCoreContext())) {
            for (org.apache.http.Header h: response.getHeaders("Set-Cookie")) {
                if (h.getValue().contains(AuthenticationSessionManager.AUTH_SESSION_ID)) {
                    cookieStore.addCookie(parseCookie(h.getValue(), AuthenticationSessionManager.AUTH_SESSION_ID));
                } else if (h.getValue().contains(KC_RESTART)) {
                    cookieStore.addCookie(parseCookie(h.getValue(), KC_RESTART));
                }
            }
        }

        return cookieStore;
    }

    private BasicClientCookie parseCookie(String line, String name) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        String path = "";
        String value = "";

        for (String s: line.split(";")) {
            if (s.contains(name)) {
                String[] split = s.split("=");
                value = split[1];
            } else if (s.contains("Path")) {
                String[] split = s.split("=");
                path = split[1];
            }
        }

        BasicClientCookie c = new BasicClientCookie(name, value);
        c.setExpiryDate(calendar.getTime());
        c.setDomain(AUTH_SERVER_HOST);
        c.setPath(path);

        return c;
    }

    private void login(String requestURI, CookieStore cookieStore) throws IOException {
        HttpCoreContext httpContext = new HttpCoreContext();
        HttpGet request = new HttpGet(requestURI);

        // send an initial request, we are redirected to login page
        String entityContent;
        try (CloseableHttpResponse response = sendRequest(request, cookieStore, httpContext)) {
            entityContent = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
        }

        // send credentials to login form
        HttpPost post = new HttpPost(ActionURIUtils.getActionURIFromPageSource(entityContent));
        List<NameValuePair> params = new LinkedList<>();
        params.add(new BasicNameValuePair("username", "foo"));
        params.add(new BasicNameValuePair("password", "password"));

        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpResponse response = sendRequest(post, cookieStore, httpContext)) {
            Assert.assertThat("Expected successful login.", response.getStatusLine().getStatusCode(), is(equalTo(200)));
        }
    }
}
