package org.keycloak.testsuite.cookies;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.Cookie;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class CookiesPathTest extends AbstractKeycloakTest {

    @Test
    public void testCookiesPath() {
        // navigate to "/realms/foo/account" and remove cookies in the browser for the current path
        // first access to the path means there are no cookies being sent
        // we are redirected to login page and Keycloak sets cookie's path to "/auth/realms/foo/"
        deleteAllCookiesForRealm("foo");

        Assert.assertTrue("There shouldn't be any cookies sent!", driver.manage().getCookies().isEmpty());

        // refresh the page and cookies are sent within the request
        driver.navigate().refresh();

        Set<Cookie> cookies = driver.manage().getCookies();
        Assert.assertTrue("There should be cookies sent!", cookies.size() > 0);
        // check cookie's path, for some reason IE adds extra slash to the beginning of the path
        cookies.stream().forEach(cookie -> Assert.assertThat(cookie.getPath(), Matchers.endsWith("/auth/realms/foo/")));

        // now navigate to realm which name overlaps the first realm and delete cookies for that realm (foobar)
        //
        deleteAllCookiesForRealm("foobar");

        // cookies shouldn't be sent for the first access to /realms/foobar/account
        // At this moment IE would sent cookies for /auth/realms/foo without the fix
        cookies = driver.manage().getCookies();
        Assert.assertTrue("There shouldn't be any cookies sent!", cookies.isEmpty());

        // refresh the page and check if correct cookies were sent
        driver.navigate().refresh();
        cookies = driver.manage().getCookies();

        Assert.assertTrue("There should be cookies sent!", cookies.size() > 0);
        // check cookie's path, for some reason IE adds extra slash to the beginning of the path
        cookies.stream().forEach(cookie -> Assert.assertThat(cookie.getPath(), Matchers.endsWith("/auth/realms/foobar/")));

        // lets back to "/realms/foo/account" to test the cookies for "foo" realm are still there and haven't been (correctly) sent to "foobar"
        URLUtils.navigateToUri( oauth.AUTH_SERVER_ROOT + "/realms/foo/account", true);

        cookies = driver.manage().getCookies();
        Assert.assertTrue("There should be cookies sent!", cookies.size() > 0);
        cookies.stream().forEach(cookie -> Assert.assertThat(cookie.getPath(), Matchers.endsWith("/auth/realms/foo/")));
    }

    /**
     * Add two realms which names are overlapping i.e foo and foobar
     * @param testRealms
     */
    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder foo = RealmBuilder.create().name("foo").testEventListener();
        foo.client(ClientBuilder.create().clientId("myclient").publicClient().directAccessGrants());
        foo.user(UserBuilder.create().username("foo").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));
        testRealms.add(foo.build());

        RealmBuilder foobar = RealmBuilder.create().name("foobar").testEventListener();
        foobar.client(ClientBuilder.create().clientId("myclient").publicClient().directAccessGrants());
        foobar.user(UserBuilder.create().username("foobar").password("password").role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN));
        testRealms.add(foobar.build());
    }
}
