package org.keycloak.testsuite.forms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.authentication.authenticators.access.AllowAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.access.DenyAccessAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserAttributeValueFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.userprofile.UserProfileUtil;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.forms.BrowserFlowTest.revertFlows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author <a href="mailto:dmartino@redhat.com">Daniele Martinoli</a>
 */
public class ConditionalUserAttributeAuthenticatorTest extends AbstractTestRealmKeycloakTest {

    private final static String X_APPROVE_ATTR = "x-approved";
    private final static String X_APPROVE_ATTR_VALUE = Boolean.toString(true);

    private final static String APPROVED_GROUP = "approved";
    private final static String SUBGROUP = "subgroup";
    
    private final static String APPROVED_USER = "approved";
    private final static String APPROVED_BY_GROUP_USER = "approved-by-group";
    private final static String APPROVED_BY_SUBGROUP_USER = "approved-by-subgroup";
    private final static String PASSWORD = generatePassword();

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected ErrorPage errorPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {}

    @Before
    public void configureUserProfile() {
        UserProfileResource userProfileRes = testRealm().users().userProfile();
        UserProfileUtil.enableUnmanagedAttributes(userProfileRes);
    }

    private void createUsers() {
        GroupRepresentation subGroup = GroupBuilder.create().name(SUBGROUP).build();
        testRealm().groups().add(subGroup);
        GroupRepresentation approvedGroup = GroupBuilder.create().name(APPROVED_GROUP).subGroups(List.of(subGroup))
            .attributes(Map.of(X_APPROVE_ATTR, List.of(X_APPROVE_ATTR_VALUE)))
            .build();
        testRealm().groups().add(approvedGroup);
        
        UserRepresentation approved = UserBuilder.create().username(APPROVED_USER).password(PASSWORD)
            .addAttribute(X_APPROVE_ATTR, X_APPROVE_ATTR_VALUE)
            .build();
        testRealm().users().create(approved);

        UserRepresentation approvedByGroup = UserBuilder.create().username(APPROVED_BY_GROUP_USER).password(PASSWORD)
            .addAttribute(X_APPROVE_ATTR, X_APPROVE_ATTR_VALUE)
            .addGroups(APPROVED_GROUP)
            .build();
        testRealm().users().create(approvedByGroup);

        UserRepresentation approvedBySubgroup = UserBuilder.create().username(APPROVED_BY_SUBGROUP_USER).password(PASSWORD)
            .addAttribute(X_APPROVE_ATTR, X_APPROVE_ATTR_VALUE)
            .addGroups(SUBGROUP)
            .build();
        testRealm().users().create(approvedBySubgroup);
    }

    @Test
    public void testAllowedUsersWithApprovedAttribute(){
        final String flowAlias = "browser - user attribute condition";
        final String errorMessage = "You don't have necessary attribute.";

        createUsers();
        configureBrowserFlowWithConditionalUserAttribute(flowAlias, errorMessage);

        for (String user : List.of(APPROVED_USER, APPROVED_BY_GROUP_USER, APPROVED_BY_SUBGROUP_USER)) {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(user);
    
            final String testUserId = testRealm().users().search(user).get(0).getId();
    
            passwordPage.assertCurrent();
            passwordPage.login(PASSWORD);

            events.expectLogin()
                    .user(testUserId)
                    .detail(Details.USERNAME, user)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();

            AccountHelper.logout(testRealm(), user);
        }
    }

    /**
     * This test checks that if user does not have specific attribute, then the access is denied.
     */
    @Test
    public void testDenyUserWithoutApprovedAttribute() {
        final String flowAlias = "browser - user attribute condition";
        final String errorMessage = "You don't have necessary attribute.";
        final String user = "test-user@localhost";

        configureBrowserFlowWithConditionalUserAttribute(flowAlias, errorMessage);

        try {
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.assertCurrent();
            loginUsernameOnlyPage.login(user);

            errorPage.assertCurrent();
            assertThat(errorPage.getError(), is(errorMessage));

            events.expectLogin()
                    .user((String) null)
                    .session((String) null)
                    .error(Errors.ACCESS_DENIED)
                    .detail(Details.USERNAME, user)
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            revertFlows(testRealm(), flowAlias);
        }
    }

    /**
     * This flow contains:
     * UsernameForm REQUIRED
     * Subflow CONDITIONAL
     * ** conditional user attribute
     * ** Allow Access REQUIRED
     * Subflow CONDITIONAL
     * ** conditional user attribute-negated
     * ** Deny Access REQUIRED
     * Password REQUIRED
     *
     * @param newFlowAlias
     * @param conditionProviderId
     * @param conditionConfig
     * @param denyConfig
     */
    private void configureBrowserFlowWithConditionalUserAttribute(String newFlowAlias, String errorMessage) {
        Map<String, String> hasApproveAttributeConfigMap = new HashMap<>();
        hasApproveAttributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_NAME, X_APPROVE_ATTR);
        hasApproveAttributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_EXPECTED_VALUE, X_APPROVE_ATTR_VALUE);
        hasApproveAttributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_INCLUDE_GROUP_ATTRIBUTES, Boolean.toString(true));
        hasApproveAttributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_NOT,  Boolean.toString(false));

        Map<String, String> missApproveAttributeConfigMap = new HashMap<>(hasApproveAttributeConfigMap);
        missApproveAttributeConfigMap.put(ConditionalUserAttributeValueFactory.CONF_NOT,  Boolean.toString(true));

        Map<String, String> denyAccessConfigMap = new HashMap<>();
        denyAccessConfigMap.put(DenyAccessAuthenticatorFactory.ERROR_MESSAGE, errorMessage);

        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .inForms(forms -> forms
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID)
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, 
                                    ConditionalUserAttributeValueFactory.PROVIDER_ID, 
                                    config -> config.setConfig(hasApproveAttributeConfigMap))
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, 
                                    AllowAccessAuthenticatorFactory.PROVIDER_ID, config -> {})
                        )
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subflow -> subflow
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, 
                                ConditionalUserAttributeValueFactory.PROVIDER_ID, 
                                config -> config.setConfig(missApproveAttributeConfigMap))
                                .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, 
                                    DenyAccessAuthenticatorFactory.PROVIDER_ID, config -> config.setConfig(denyAccessConfigMap))
                        )
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID)
                )
                .defineAsBrowserFlow() // Activate this new flow
        );
    }
}
