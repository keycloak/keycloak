package org.keycloak.tests.forms;

import java.time.Duration;
import java.util.Map;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.tests.common.BasicUserConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest
public class AuthCookieAndPasswordLoginTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectUser(config = BasicUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser testUser;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    PasswordPage passwordPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @TestSetup
    public void setup() {
        AuthenticationManagementResource flows = managedRealm.admin().flows();
        AuthenticationFlowRepresentation flowRep = newFlow("cookie-password-browser", "Cookie and password browser", "basic-flow", true, false);
        // create parent flow and the sub-flowas as alternative
        flows.createFlow(flowRep);
        flows.addExecutionFlow("cookie-password-browser", Map.of("alias", "step-cookie", "type", "basic-flow"));
        flows.addExecutionFlow("cookie-password-browser", Map.of("alias", "step-forms", "type", "basic-flow"));
        for (AuthenticationExecutionInfoRepresentation exec : flows.getExecutions("cookie-password-browser")) {
            exec.setRequirement(AuthenticationExecutionModel.Requirement.ALTERNATIVE.name());
            flows.updateExecutions("cookie-password-browser", exec);
        }

        // add executions to step-cookie as required
        flows.addExecution("step-cookie", Map.of("provider", CookieAuthenticatorFactory.PROVIDER_ID));
        flows.addExecution("step-cookie", Map.of("provider", PasswordFormFactory.PROVIDER_ID));
        for (AuthenticationExecutionInfoRepresentation exec : flows.getExecutions("step-cookie")) {
            exec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            flows.updateExecutions("step-cookie", exec);
        }

        // add execution to step-forms as required
        flows.addExecution("step-forms", Map.of("provider", UsernamePasswordFormFactory.PROVIDER_ID));
        for (AuthenticationExecutionInfoRepresentation exec : flows.getExecutions("step-cookie")) {
            exec.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
            flows.updateExecutions("step-forms", exec);
        }
        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        realmRep.setBrowserFlow("cookie-password-browser");
        managedRealm.admin().update(realmRep);
    }

    @Test
    public void cookieAndPasswordLogin() {
        // login normally using the username and password page
        oauth.doLogin(testUser.getUsername(), testUser.getPassword());
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        // using cookie and password
        oauth.openLoginForm();
        passwordPage.assertCurrent();
        Assertions.assertEquals(testUser.getUsername(), passwordPage.getAttemptedUsername());
        passwordPage.fillPassword(testUser.getPassword());
        passwordPage.submit();
        Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());

        runOnServer.run(InfinispanTimeUtil.enableTestingTimeService());
        try {
            // expire the login using 10 seconds for login
            managedRealm.updateWithCleanup(r -> r.accessCodeLifespanUserAction(10).accessCodeLifespanLogin(10));
            oauth.openLoginForm();
            passwordPage.assertCurrent();
            Assertions.assertEquals(testUser.getUsername(), passwordPage.getAttemptedUsername());
            timeOffSet.set(Duration.ofSeconds(70));
            passwordPage.fillPassword(testUser.getPassword());
            passwordPage.submit();
            Assertions.assertEquals("Your login attempt timed out. Login will start from the beginning.",
                    passwordPage.getError());

            // login again should success
            passwordPage.fillPassword(testUser.getPassword());
            passwordPage.submit();
            Assertions.assertTrue(oauth.parseLoginResponse().isSuccess());
        } finally {
            runOnServer.run(InfinispanTimeUtil.disableTestingTimeService());
        }
    }

    private AuthenticationFlowRepresentation newFlow(String alias, String description,
            String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }
}
