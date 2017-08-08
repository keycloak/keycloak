package org.keycloak.testsuite.springboot;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.util.TokenUtil;
import org.openqa.selenium.By;

public class TokenSpringBootTest extends AbstractSpringBootTest {
    @Test
    public void testTokens() {
        String servletUri = APPLICATION_URL + "/admin/TokenServlet";

        driver.navigate().to(servletUri + "?" + OAuth2Constants.SCOPE + "=" + OAuth2Constants.OFFLINE_ACCESS);

        Assert.assertTrue("Must be on login page", loginPage.isCurrent());
        loginPage.login(USER_LOGIN, USER_PASSWORD);

        WaitUtils.waitUntilElement(By.tagName("body")).is().visible();

        Assert.assertTrue(tokenPage.isCurrent());

        Assert.assertEquals(tokenPage.getRefreshToken().getType(), TokenUtil.TOKEN_TYPE_OFFLINE);
        Assert.assertEquals(tokenPage.getRefreshToken().getExpiration(), 0);

        String accessTokenId = tokenPage.getAccessToken().getId();
        String refreshTokenId = tokenPage.getRefreshToken().getId();

        setAdapterAndServerTimeOffset(9999, servletUri);

        driver.navigate().to(servletUri);
        Assert.assertTrue("Must be on tokens page", tokenPage.isCurrent());
        Assert.assertNotEquals(tokenPage.getRefreshToken().getId(), refreshTokenId);
        Assert.assertNotEquals(tokenPage.getAccessToken().getId(), accessTokenId);

        setAdapterAndServerTimeOffset(0, servletUri);

        driver.navigate().to(logoutPage(servletUri));
        Assert.assertTrue("Must be on login page", loginPage.isCurrent());
    }
}
