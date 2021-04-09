package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.access.DenyAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalRoleAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.authentication.ConditionalUserAttributeValueFactory;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FlowUtil;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.forms.BrowserFlowTest.revertFlows;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@AuthServerContainerExclude(REMOTE)
public class AllowDenyAuthenticatorTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected ErrorPage errorPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void testDenyAccessWithDefaultMessage() {
        testErrorMessageInDenyAccess(null, "Access denied");
    }

    @Test
    public void testDenyAccessWithParticularMessage() {
        final String message = "You are not allowed to authenticate.";
        testErrorMessageInDenyAccess(message, message);
    }

    @Test
    public void testDenyAccessWithProperty() {
        final String property = "brokerLinkingSessionExpired";
        final String message = "Requested broker account linking, but current session is no longer valid.";

        testErrorMessageInDenyAccess(property, message);
    }

    @Test
    public void testDenyAccessWithNotExistingProperty() {
        final String property = "not-existing-property";
        final String message = "not-existing-property";

        testErrorMessageInDenyAccess(property, message);
    }

    /* Helper method for error messaged in Deny Authenticator */
    private void testErrorMessageInDenyAccess(String setUpMessage, String expectedMessage) {
        final String flowAlias = "browser - deny defaultMessage";
        final String userWithoutAttribute = "test-user@localhost";

        Map<String, String> denyAccessConfigMap = new HashMap<>();
        if (setUpMessage != null) {
            denyAccessConfigMap.put(DenyAccessAuthenticatorFactory.ERROR_MESSAGE, setUpMessage);
        }

        configureBrowserFlowWithDenyAccess(flowAlias, denyAccessConfigMap);

        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(userWithoutAttribute);

            errorPage.assertCurrent();
            assertThat(errorPage.getError(), is(expectedMessage));

            events.expectLogin()
                    .user((String) null)
                    .session((String) null)
                    .error(Errors.ACCESS_DENIED)
                    .detail(Details.USERNAME, userWithoutAttribute)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            revertFlows(testRealm(), flowAlias);
        }
    }

    /**
     * This test checks that if user does not have specific attribute, then the access is denied.
     */
    @Test
    public void testDenyAccessWithNegateUserAttributeCondition() {
        final String flowAlias = "browser - user attribute condition";
        final String userWithoutAttribute = "test-user@localhost";
        final String errorMessage = "You don't have necessary attribute.";

        Map<String, String> attributeConfigMap = new HashMap<>();
        attributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_NAME, "attribute");
        attributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_EXPECTED_VALUE, "value");
        attributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_NOT, "true");

        Map<String, String> denyAccessConfigMap = new HashMap<>();
        denyAccessConfigMap.put(DenyAccessAuthenticatorFactory.ERROR_MESSAGE, errorMessage);

        configureBrowserFlowWithDenyAccessInConditionalFlow(flowAlias, ConditionalUserAttributeValueFactory.PROVIDER_ID, attributeConfigMap, denyAccessConfigMap);

        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(userWithoutAttribute);

            errorPage.assertCurrent();
            assertThat(errorPage.getError(), is(errorMessage));

            events.expectLogin()
                    .user((String) null)
                    .session((String) null)
                    .error(Errors.ACCESS_DENIED)
                    .detail(Details.USERNAME, userWithoutAttribute)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            revertFlows(testRealm(), flowAlias);
        }
    }

    /**
     * Deny access, if user has defined the role and print error message.
     */
    @Test
    public void testDenyAccessWithRoleCondition() {
        denyAccessWithRoleCondition(false);
    }

    /**
     * Deny access, if user has NOT defined the role and print error message.
     */
    @Test
    public void testDenyAccessWithNegateRoleCondition() {
        denyAccessWithRoleCondition(true);
    }

    /**
     * Helper method for deny access with role condition
     *
     * @param negateOutput
     */
    private void denyAccessWithRoleCondition(boolean negateOutput) {
        final String flowAlias = "browser-deny";
        final String userWithRole = "test-user@localhost";
        final String userWithoutRole = "john-doh@localhost";
        final String role = "offline_access";
        final String errorMessage = "Your account doesn't have the required role";

        Map<String, String> config = new HashMap<>();
        config.put(ConditionalRoleAuthenticatorFactory.CONDITIONAL_USER_ROLE, role);
        config.put(ConditionalRoleAuthenticatorFactory.CONF_NEGATE, Boolean.toString(negateOutput));

        Map<String, String> denyConfig = new HashMap<>();
        denyConfig.put(DenyAccessAuthenticatorFactory.ERROR_MESSAGE, errorMessage);

        configureBrowserFlowWithDenyAccessInConditionalFlow(flowAlias, ConditionalRoleAuthenticatorFactory.PROVIDER_ID, config, denyConfig);

        denyAccessInConditionalFlow(flowAlias,
                negateOutput ? userWithoutRole : userWithRole,
                negateOutput ? userWithRole : userWithoutRole,
                errorMessage
        );
    }

    /**
     * Helper method for deny access with two opposites cases
     */
    private void denyAccessInConditionalFlow(String flowAlias, String userCondMatch, String userCondNotMatch, String errorMessage) {
        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(userCondMatch);

            errorPage.assertCurrent();
            assertThat(errorPage.getError(), is(errorMessage));

            events.expectLogin()
                    .user((String) null)
                    .session((String) null)
                    .error(Errors.ACCESS_DENIED)
                    .detail(Details.USERNAME, userCondMatch)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();

            final String userCondNotMatchId = testRealm().users().search(userCondNotMatch).get(0).getId();

            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(userCondNotMatch);

            passwordPage.assertCurrent();
            passwordPage.login("password");

            events.expectLogin().user(userCondNotMatchId)
                    .detail(Details.USERNAME, userCondNotMatch)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            revertFlows(testRealm(), flowAlias);
        }
    }

    /**
     * This test checks that if user has NOT the required role, the user has to enter the password
     */
    @Test
    public void testSkipExecutionUserHasNotRoleCondition() {
        final String userWithoutRole = "john-doh@localhost";
        final String role = "offline_access";
        final String newFlowAlias = "browser - allow skip";

        Map<String, String> configMap = new HashMap<>();
        configMap.put(ConditionalRoleAuthenticatorFactory.CONDITIONAL_USER_ROLE, role);
        configMap.put(ConditionalRoleAuthenticatorFactory.CONF_NEGATE, "false");

        configureBrowserFlowWithSkipExecutionInConditionalFlow(newFlowAlias, ConditionalRoleAuthenticatorFactory.PROVIDER_ID, configMap);
        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(userWithoutRole);

            final String testUserWithoutRoleId = testRealm().users().search(userWithoutRole).get(0).getId();

            passwordPage.assertCurrent();
            passwordPage.login("password");

            events.expectLogin()
                    .user(testUserWithoutRoleId)
                    .detail(Details.USERNAME, userWithoutRole)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            revertFlows(testRealm(), newFlowAlias);
        }
    }

    /**
     * This test checks that if user has the required role, the user skips the other executions
     */
    @Test
    public void testSkipOtherExecutionsIfUserHasRoleCondition() {
        final String userWithRole = "test-user@localhost";
        final String role = "offline_access";
        final String newFlowAlias = "browser - allow skip";

        Map<String, String> configMap = new HashMap<>();
        configMap.put(ConditionalRoleAuthenticatorFactory.CONDITIONAL_USER_ROLE, role);
        configMap.put(ConditionalRoleAuthenticatorFactory.CONF_NEGATE, "false");

        configureBrowserFlowWithSkipExecutionInConditionalFlow(newFlowAlias, ConditionalRoleAuthenticatorFactory.PROVIDER_ID, configMap);
        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(userWithRole);

            final String testUserWithRoleId = testRealm().users().search(userWithRole).get(0).getId();

            events.expectLogin()
                    .user(testUserWithRoleId)
                    .detail(Details.USERNAME, userWithRole)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            revertFlows(testRealm(), newFlowAlias);
        }
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Subflow CONDITIONAL
     * ** condition
     * ** Deny Access REQUIRED
     * Password REQUIRED
     *
     * @param newFlowAlias
     * @param conditionProviderId
     * @param conditionConfig
     * @param denyConfig
     */
    private void configureBrowserFlowWithDenyAccessInConditionalFlow(String newFlowAlias, String conditionProviderId, Map<String, String> conditionConfig, Map<String, String> denyConfig) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, conditionProviderId, config -> config.setConfig(conditionConfig))
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, DenyAccessAuthenticatorFactory.PROVIDER_ID, config -> config.setConfig(denyConfig))
                        )
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Deny Access REQUIRED
     *
     * @param newFlowAlias
     * @param denyConfig
     */
    private void configureBrowserFlowWithDenyAccess(String newFlowAlias, Map<String, String> denyConfig) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, DenyAccessAuthenticatorFactory.PROVIDER_ID, config -> config.setConfig(denyConfig))
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Subflow REQUIRED
     * ** subflow ALTERNATIVE
     * *** conditional-subflow CONDITIONAL
     * **** condition REQUIRED
     * **** Allow Access REQUIRED
     * ** Password ALTERNATIVE
     *
     * @param newFlowAlias
     * @param conditionProviderId
     * @param configMap
     */
    private void configureBrowserFlowWithSkipExecutionInConditionalFlow(String newFlowAlias, String conditionProviderId, Map<String, String> configMap) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.REQUIRED, subflow -> subflow
                                .addSubFlowExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, flow -> flow
                                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, condFlow -> condFlow
                                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, conditionProviderId, config -> config.setConfig(configMap))
                                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, AllowAccessAuthenticatorFactory.PROVIDER_ID)))
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, PasswordFormFactory.PROVIDER_ID)

                        ))
                .defineAsBrowserFlow() // Activate this new flow
        );
    }
}
