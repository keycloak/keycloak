package org.keycloak.testsuite.util;

import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

public class TestAppHelper {
    private final OAuthClient oauth;
    private final LoginPage loginPage;
    private final LoginTotpPage loginTotpPage;

    private String refreshToken;

    public TestAppHelper(OAuthClient oauth, LoginPage loginPage) {
        this(oauth, loginPage, null);
    }

    public TestAppHelper(OAuthClient oauth, LoginPage loginPage, LoginTotpPage loginTotpPage) {
        this.oauth = oauth;
        this.loginPage = loginPage;
        this.loginTotpPage = loginTotpPage;
    }

    public void login(String username, String password) {
        startLogin(username, password);
        completeLogin();
    }

    public void startLogin(String username, String password) {
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();
    }

    public void completeLogin() {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        refreshToken = tokenResponse.getRefreshToken();
    }

    public void login(String username, String password, String otp) {
        if (loginTotpPage == null) {
            throw new IllegalStateException("TOTP page not configured");
        }
        startLogin(username, password);
        loginTotpPage.login(otp);
        completeLogin();
    }

    public void login(String username, String password, String realm, String clientId, String idp) {
        oauth.client(clientId);
        oauth.realm(realm);
        oauth.openLoginForm();
        loginPage.clickSocial(idp);
        loginPage.fillLogin(username, password);
        loginPage.submit();
        completeLogin();
    }

    public boolean logout() {
        try {
            return oauth.doLogout(refreshToken).isSuccess();
        } catch (RuntimeException e) {
            return false;
        }
    }
}
