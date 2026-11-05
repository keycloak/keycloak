package org.keycloak.tests.forms;

import java.util.List;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.InfoPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.tests.actions.AbstractAppInitiatedActionTest;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.testsuite.util.FlowUtil;

@KeycloakIntegrationTest
public abstract class AbstractResetCredentialsAlternativeFlowsTest extends AbstractAppInitiatedActionTest {

    @InjectUser(config = TestRealmUserConfig.class)
    protected ManagedUser managedUser;

    @InjectRunOnServer
    protected RunOnServerClient runOnServer;

    @InjectMailServer
    MailServer mail;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    InfoPage infoPage;

    @InjectPage
    protected LoginPasswordResetPage resetPasswordPage;

    void revertResetCredentialsFlows() {
        runOnServer.run(session -> {
            AuthenticationFlowModel alternativeResetCredentialsFlow = session.getContext().getRealm().getResetCredentialsFlow();
            FlowUtil.inCurrentRealm(session).selectFlow(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW).defineAsResetCredentialsFlow();
            session.getContext().getRealm().removeAuthenticationFlow(alternativeResetCredentialsFlow);
        });
    }


    // Create a copy of the default reset credentials flow with the specified flow alias if it doesn't exist yet
    // Remove execution(s), specified by (a list of) providerId(s) from the flow
    // Finally bind / define the flow as the reset credential one
    void configureResetCredentialsRemoveExecutionsAndBindTheFlow(String newFlowAlias, List<String> providerIdsToRemove) {
        runOnServer.run(session -> {
            // Create a copy of the default reset credentials flow with the specified flow alias if it doesn't exist yet
            if(session.getContext().getRealm().getFlowByAlias(newFlowAlias) == null) {
                FlowUtil.inCurrentRealm(session).copyResetCredentialsFlow(newFlowAlias);
            }
        });

        for(String providerId : providerIdsToRemove) {
            // For each execution to be removed its index within the flow based on providerId
            int executionIndex = managedRealm.admin().flows().getExecutions(newFlowAlias)
                    .stream()
                    .filter(e -> e.getProviderId() != null && e.getProviderId().equals(providerId))
                    .mapToInt(e -> e.getIndex())
                    .findFirst()
                    .getAsInt();

            // Remove the execution(s)
            runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .removeExecution(executionIndex)
            );
        }

        // Bind the flow as the reset-credentials one
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .defineAsResetCredentialsFlow()
        );
    }
}
