package org.keycloak.tests.organization.authentication;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.authentication.authenticators.browser.OTPFormAuthenticatorFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.common.util.UriUtils;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginTotpPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.SelectOrganizationPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.util.FlowUtil;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class OrganizationAuthenticationTest extends AbstractOrganizationTest {

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginUsernamePage loginUsernamePage;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginPasswordResetPage loginPasswordResetPage;

    @InjectPage
    LoginTotpPage loginTotpPage;

    @InjectPage
    SelectOrganizationPage selectOrganizationPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private static final String STEPUP_BROWSER_FLOW = "stepup-loa-browser";
    private static final String ATTACKER_EMAIL = "attacker@attacker.test";
    private static final String ATTACKER_TOTP_SECRET = "attackerotpsecret";
    private static final String VICTIM_TOTP_SECRET = "victimotpsecret99";

    private final TimeBasedOTP totp = new TimeBasedOTP();

    @BeforeEach
    public void resetOAuthScope() {
        oauth.scope(null);
    }

    @Test
    public void testStaleSsoAuthNoteAccountTakeover() {
        // allow the reset-credentials transition used to clear the authenticated user mid-session
        realm.updateWithCleanup(r -> r.resetPasswordAllowed(true));

        // attacker: member of two orgs (forced to the select-organization challenge, which keeps SSO_AUTH set)
        // victim: member of a single org whose email domain resolves to that org
        StaleNoteAttackContext ctx = createStaleNoteAttackContext(null, null);
        Runnable openLogin = () -> {
            oauth.scope("organization");
            oauth.openLoginForm();
        };

        // 1) attacker logs in to establish an SSO session and harvest their identity cookie
        openLogin.run();
        loginAttackerThroughSelectOrganization(ctx);
        assertLoginSuccess();
        Cookie identityCookie = harvestIdentityCookieAndClearSession();

        // 2-5) in a fresh session: restart the flow with the attacker cookie so the cookie authenticator stamps
        // SSO_AUTH with the attacker id, clear the authenticated user through reset-credentials and then submit the
        // victim's username on the identity-first page.
        //
        // Secure expectation: the authentication session still carries the attacker's SSO_AUTH note (never removed
        // by clearUser). Re-authentication must only be trusted for the user it was started for, so binding that note
        // to a now-different user (the victim) must abort the flow instead of silently reusing the attacker's prior
        // authentication state. No authorization code is issued for the victim.
        replayStaleNotesAgainstVictim(openLogin, identityCookie, ctx.victimEmail());
    }

    @Test
    public void testStaleLoaMapStepUpBypass() {
        // allow the reset-credentials transition used to clear the authenticated user mid-session
        realm.updateWithCleanup(r -> r.resetPasswordAllowed(true));

        // add a LoA-based OTP step-up to the browser flow: when an authentication of level >= 2 is requested, the
        // OTP form is required UNLESS level 2 was already reached in a previous authentication. "Previous level"
        // is read from the per-level LOA_MAP note, which the cookie authenticator copies from the identity cookie.
        addLoaOtpStepUp();

        // attacker holds their OWN OTP credential, so they can legitimately reach level 2; the victim is protected
        // by an OTP the attacker does NOT know (the attacker only needs the victim's password)
        StaleNoteAttackContext ctx = createStaleNoteAttackContext(ATTACKER_TOTP_SECRET, VICTIM_TOTP_SECRET);
        Runnable openLogin = this::openOrgLoginRequestingLoa2;

        // 1) attacker performs a full level-2 login (password + their own OTP). This records level 2 in the
        // LOA_MAP note of the attacker's SSO session, which the attacker identity cookie references.
        openLogin.run();
        loginAttackerThroughSelectOrganization(ctx);
        loginTotpPage.assertCurrent();
        // refresh the OTP generator clock just before use so it stays aligned with the server
        setOtpTimeOffset();
        loginTotpPage.login(totp.generateTOTP(ATTACKER_TOTP_SECRET));
        assertLoginSuccess();
        Cookie identityCookie = harvestIdentityCookieAndClearSession();

        // 2-5) in a fresh session: restart the level-2 flow with the attacker cookie so the cookie authenticator copies
        // the attacker's LOA_MAP (containing level 2) into the authentication session, clear the authenticated user
        // through reset-credentials and then submit the victim's username on the identity-first page.
        //
        // Secure expectation: same as above, the stale SSO_AUTH note bound to a different user aborts the flow, so the
        // attacker can never reuse the attacker's LOA_MAP to skip the victim's OTP.
        replayStaleNotesAgainstVictim(openLogin, identityCookie, ctx.victimEmail());
    }

    // --- Helper methods ---

    private record StaleNoteAttackContext(String attackerEmail, String victimEmail, String orgAlias) {
    }

    /**
     * Two organizations, an attacker that is a member of both (forced to the select-organization challenge, which keeps
     * the cookie authenticator from completing the flow so it only stamps its notes and defers) and a victim that is a
     * member of a single org whose email domain resolves to that org.
     *
     * @param attackerTotpSecret optional TOTP secret for the attacker, {@code null} for password only
     * @param victimTotpSecret   optional TOTP secret for the victim, {@code null} for password only
     */
    private StaleNoteAttackContext createStaleNoteAttackContext(String attackerTotpSecret, String victimTotpSecret) {
        OrganizationRepresentation orgA = createOrganization();
        OrganizationRepresentation orgB = createOrganization("org-b");
        OrganizationResource orgAResource = realm.admin().organizations().get(orgA.getId());
        OrganizationResource orgBResource = realm.admin().organizations().get(orgB.getId());

        // attacker email domain matches neither org
        String attackerId = createOrgMember(orgAResource, ATTACKER_EMAIL, attackerTotpSecret);
        orgBResource.members().addMember(attackerId).close();

        String victimEmail = "victim@" + orgA.getName() + ".org";
        createOrgMember(orgAResource, victimEmail, victimTotpSecret);

        return new StaleNoteAttackContext(ATTACKER_EMAIL, victimEmail, orgA.getAlias());
    }

    /**
     * Identity-first login as the attacker up to and including the password form.
     */
    private void loginAttackerThroughSelectOrganization(StaleNoteAttackContext ctx) {
        loginUsernamePage.fillLoginWithUsernameOnly(ctx.attackerEmail());
        loginUsernamePage.submit();
        selectOrganizationPage.assertCurrent();
        selectOrganizationPage.selectOrganization(ctx.orgAlias());
        loginPage.assertCurrent();
        loginPage.fillPassword(memberPassword);
        loginPage.submit();
    }

    /**
     * Reads the (HttpOnly) attacker identity cookie from the Keycloak domain and then drops all cookies: the attack
     * runs in a FRESH session, so the root authentication session gets a random id with no bound user session and the
     * completion-time DIFFERENT_USER_AUTHENTICATED guard alone has nothing to compare against.
     */
    private Cookie harvestIdentityCookieAndClearSession() {
        driver.open(oauth.getEndpoints().getIssuer());
        Cookie identityCookie = driver.driver().manage().getCookieNamed("KEYCLOAK_IDENTITY");
        assertNotNull(identityCookie, "attacker identity cookie not established");
        driver.driver().manage().deleteAllCookies();
        return identityCookie;
    }

    /**
     * Replays the stale-notes attack and asserts it is rejected:
     * <ol>
     * <li>start a new request with no cookies -> identity-first username page, extract the login-actions URLs</li>
     * <li>re-inject the attacker identity cookie and restart the flow: resetFlow clears the execution statuses, so the
     * cookie authenticator runs again and stamps its notes (SSO_AUTH, LOA_MAP) for the attacker, then defers to the
     * select-organization challenge. The identity cookie is dropped again.</li>
     * <li>clear the authenticated user without finalizing the flow: the reset-credentials choose-user form renders (no
     * identity cookie) and submitting an unknown username triggers clearUser(). The notes stay set on the still-open
     * authentication session.</li>
     * <li>re-enter the identity-first username page and submit the victim's username</li>
     * <li>expect the error page and no authorization code</li>
     * </ol>
     */
    private void replayStaleNotesAgainstVictim(Runnable openLogin, Cookie identityCookie, String victimEmail) {
        openLogin.run();
        loginUsernamePage.assertCurrent();

        String flowUrl = driver.driver().findElements(By.tagName("form")).stream()
                .map(f -> f.getAttribute("action"))
                .filter(a -> a != null && a.contains("/login-actions/"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no login-actions form found on identity-first page"));
        String clientId = queryParam(flowUrl, OAuth2Constants.CLIENT_ID);
        String tabId = queryParam(flowUrl, "tab_id");
        String clientData = queryParam(flowUrl, "client_data");
        String loginActionsBase = flowUrl.substring(0, flowUrl.indexOf("/login-actions/")) + "/login-actions/";
        String restartUrl = loginAction(loginActionsBase, "restart", clientId, tabId, clientData);
        String resetUrl = loginAction(loginActionsBase, "reset-credentials", clientId, tabId, clientData);
        String authenticateUrl = loginAction(loginActionsBase, "authenticate", clientId, tabId, clientData);

        driver.driver().manage().addCookie(identityCookie);
        driver.open(restartUrl);
        selectOrganizationPage.assertCurrent();
        driver.driver().manage().deleteCookieNamed("KEYCLOAK_IDENTITY");
        driver.driver().manage().deleteCookieNamed("KEYCLOAK_IDENTITY_LEGACY");

        driver.open(resetUrl);
        loginPasswordResetPage.assertCurrent();
        loginPasswordResetPage.changePassword("does-not-exist@nowhere.test");

        driver.open(authenticateUrl);
        loginUsernamePage.assertCurrent();
        loginUsernamePage.fillLoginWithUsernameOnly(victimEmail);
        loginUsernamePage.submit();

        errorPage.assertCurrent();
        assertThat(errorPage.getError(), Matchers.containsString("Invalid username or password"));
        assertNull(queryParam(driver.getCurrentUrl(), "code"),
                "no authorization code should be issued for the victim without a credential");
    }

    private void setOtpTimeOffset() {
        int offset = TimeBasedOTP.DEFAULT_INTERVAL_SECONDS;
        timeOffSet.set(offset);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, offset);
        totp.setCalendar(calendar);
    }

    private void openOrgLoginRequestingLoa2() {
        ClaimsRepresentation.ClaimValue<String> acr = new ClaimsRepresentation.ClaimValue<>();
        acr.setEssential(true);
        acr.setValues(List.of("2"));
        ClaimsRepresentation claims = new ClaimsRepresentation();
        claims.setIdTokenClaims(Map.of(IDToken.ACR, acr));
        oauth.loginForm().scope("openid", "organization").claims(claims).open();
    }

    private String createOrgMember(OrganizationResource organization, String email, String totpSecret) {
        UserRepresentation user = new UserRepresentation();
        user.setEmail(email);
        user.setUsername(email);
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setFirstName("First");
        user.setLastName("Last");
        List<CredentialRepresentation> credentials = new ArrayList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(memberPassword);
        credentials.add(password);
        if (totpSecret != null) {
            credentials.add(ModelToRepresentation.toRepresentation(
                    OTPCredentialModel.createTOTP(totpSecret, 6, 30, HmacOTP.HMAC_SHA1)));
        }
        user.setCredentials(credentials);

        String userId;
        try (Response response = realm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }
        realm.cleanup().add(r -> {
            try {
                r.users().get(userId).remove();
            } catch (NotFoundException ignored) {
            }
        });
        organization.members().addMember(userId).close();
        return userId;
    }

    private void addLoaOtpStepUp() {
        // Built-in flows cannot be modified, so copy the browser flow and edit the copy. copyBrowserFlow() is
        // idempotent: it restores the default binding and drops any previous copy with the same alias first.
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .copyBrowserFlow(STEPUP_BROWSER_FLOW)
                .inForms(forms -> {
                    // Disable the built-in conditional 2FA subflow so the LoA step-up below is the ONLY OTP gate;
                    // otherwise a user with an OTP credential would be prompted for OTP unconditionally, masking the
                    // LoA behaviour. The organization identity-first authenticator in this subflow must be kept.
                    List<AuthenticationExecutionModel> executions = forms.getExecutions();
                    for (int i = 0; i < executions.size(); i++) {
                        String flowId = executions.get(i).getFlowId();
                        if (flowId != null && forms.getRealm().getAuthenticationFlowById(flowId).getAlias()
                                .endsWith("Conditional 2FA")) {
                            forms.updateExecution(i, e -> e.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED));
                        }
                    }

                    // CONDITIONAL subflow: run only when a level >= 2 authentication is required and not yet satisfied
                    forms.addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                            .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED,
                                    ConditionalLoaAuthenticatorFactory.PROVIDER_ID, config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.MAX_AGE, "36000");
                                    })
                            // the second factor enforced by the step-up
                            .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED,
                                    OTPFormAuthenticatorFactory.PROVIDER_ID));
                })
                .defineAsBrowserFlow());
    }

    private void assertLoginSuccess() {
        assertNotNull(oauth.parseLoginResponse().getCode());
    }

    private String loginAction(String base, String path, String clientId, String tabId, String clientData) {
        String url = base + path + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&tab_id=" + URLEncoder.encode(tabId, StandardCharsets.UTF_8);
        if (clientData != null) {
            url += "&client_data=" + URLEncoder.encode(clientData, StandardCharsets.UTF_8);
        }
        return url;
    }

    private String queryParam(String url, String name) {
        return UriUtils.parseQueryParameters(
                URI.create(url).getRawQuery(), true).getFirst(name);
    }
}
