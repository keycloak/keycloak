package org.keycloak.testsuite.util;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.services.resources.admin.AuthenticationManagementResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public class FlowUtil {
    private RealmModel realm;
    private AuthenticationFlowModel currentFlow;
    private String flowAlias;
    private int maxPriority = 0;
    private Random rand = new Random(System.currentTimeMillis());
    private List<AuthenticationExecutionModel> executions = null;

    public class FlowUtilException extends RuntimeException {
        private static final long serialVersionUID = 5118401044519260295L;

        public FlowUtilException(String message) {
            super(message);
        }
    }

    public FlowUtil(RealmModel realm) {
        this.realm = realm;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public AuthenticationFlowModel build() {
        return currentFlow;
    }

    public static FlowUtil inCurrentRealm(KeycloakSession session) {
        return new FlowUtil(session.getContext().getRealm());
    }

    private FlowUtil newFlowUtil(AuthenticationFlowModel flowModel) {
        FlowUtil subflow = new FlowUtil(realm);
        subflow.currentFlow = flowModel;
        return subflow;
    }

    public FlowUtil selectFlow(String flowAlias) {
        currentFlow = realm.getFlowByAlias(flowAlias);
        if (currentFlow == null) {
            throw new FlowUtilException("Can't select flow: " + flowAlias + " does not exist");
        }
        this.flowAlias = flowAlias;

        return this;
    }

    public FlowUtil copyBrowserFlow(String newFlowAlias) {
        return copyFlow(DefaultAuthenticationFlows.BROWSER_FLOW, newFlowAlias);
    }

    public FlowUtil copyResetCredentialsFlow(String newFlowAlias) {
        return copyFlow(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, newFlowAlias);
    }

    public FlowUtil copyFirstBrokerLoginFlow(String newFlowAlias) {
        return copyFlow(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, newFlowAlias);
    }

    public FlowUtil copyFlow(String original, String newFlowAlias) {
        flowAlias = newFlowAlias;
        AuthenticationFlowModel existingBrowserFlow = realm.getFlowByAlias(original);
        if (existingBrowserFlow == null) {
            throw new FlowUtilException("Can't copy flow: " + original + " does not exist");
        }
        currentFlow = AuthenticationManagementResource.copyFlow(realm, existingBrowserFlow, newFlowAlias);

        return this;
    }

    public FlowUtil inForms(Consumer<FlowUtil> subFlowInitializer) {
        return inFlow(flowAlias + " forms", subFlowInitializer);
    }

    public FlowUtil inVerifyExistingAccountByReAuthentication(Consumer<FlowUtil> subFlowInitializer) {
        return inFlow(flowAlias + " Verify Existing Account by Re-authentication", subFlowInitializer);
    }

    public FlowUtil inFlow(String alias, Consumer<FlowUtil> subFlowInitializer) {
        if (subFlowInitializer != null) {
            AuthenticationFlowModel flow = realm.getFlowByAlias(alias);
            if (flow == null) {
                throw new FlowUtilException("Can't find flow by alias: " + alias);
            }
            FlowUtil subFlow = newFlowUtil(flow);
            subFlowInitializer.accept(subFlow);
        }

        return this;
    }

    public FlowUtil clear() {
        // Get executions from current flow
        List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutions(currentFlow.getId());
        // Remove all executions
        for (AuthenticationExecutionModel authExecution : executions) {
            realm.removeAuthenticatorExecution(authExecution);
        }

        return this;
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId) {
        return addAuthenticatorExecution(requirement, providerId, null);
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId, int priority) {
        return addAuthenticatorExecution(requirement, providerId, priority, null);
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId, Consumer<AuthenticatorConfigModel> configInitializer) {
        return addAuthenticatorExecution(requirement, providerId, maxPriority + 10, configInitializer);
    }

    public FlowUtil addAuthenticatorExecution(Requirement requirement, String providerId, int priority, Consumer<AuthenticatorConfigModel> configInitializer) {
        maxPriority = Math.max(maxPriority, priority);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setRequirement(requirement);
        execution.setAuthenticatorFlow(false);
        execution.setAuthenticator(providerId);
        execution.setPriority(priority);
        execution.setParentFlow(currentFlow.getId());
        if (configInitializer != null) {
            AuthenticatorConfigModel authConfig = new AuthenticatorConfigModel();
            authConfig.setId(UUID.randomUUID().toString());
            // Caller is free to update this alias
            authConfig.setAlias("cfg" + authConfig.getId().hashCode());
            authConfig.setConfig(new HashMap<>());
            configInitializer.accept(authConfig);
            realm.addAuthenticatorConfig(authConfig);

            execution.setAuthenticatorConfig(authConfig.getId());
        }
        realm.addAuthenticatorExecution(execution);

        return this;
    }

    public FlowUtil defineAsBrowserFlow() {
        realm.setBrowserFlow(currentFlow);
        return this;
    }

    public FlowUtil defineAsDirectGrantFlow() {
        realm.setDirectGrantFlow(currentFlow);
        return this;
    }

    public FlowUtil defineAsResetCredentialsFlow() {
        realm.setResetCredentialsFlow(currentFlow);
        return this;
    }

    public FlowUtil usesInIdentityProvider(String idpAlias) {
        // Setup new FirstBrokerLogin flow to identity provider
        IdentityProviderModel idp = realm.getIdentityProviderByAlias(idpAlias);
        idp.setFirstBrokerLoginFlowId(currentFlow.getId());
        realm.updateIdentityProvider(idp);
        return this;
    }

    public FlowUtil addSubFlowExecution(Requirement requirement, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution("sf" + rand.nextInt(), AuthenticationFlow.BASIC_FLOW, requirement, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(String alias, String providerId, Requirement requirement, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution(alias, providerId, requirement, maxPriority + 10, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(String alias, String providerId, Requirement requirement, int priority, Consumer<FlowUtil> flowInitializer) {
        AuthenticationFlowModel flowModel = createFlowModel(alias, providerId, null, false, false);
        return addSubFlowExecution(flowModel, requirement, priority, flowInitializer);
    }

    public static AuthenticationFlowModel createFlowModel(String alias, String providerId, String desc, boolean topLevel, boolean builtIn) {
        AuthenticationFlowModel flowModel = new AuthenticationFlowModel();
        flowModel.setId(UUID.randomUUID().toString());
        flowModel.setAlias(alias);
        flowModel.setDescription(desc);
        flowModel.setProviderId(providerId);
        flowModel.setTopLevel(topLevel);
        flowModel.setBuiltIn(builtIn);
        return flowModel;
    }

    public FlowUtil addSubFlowExecution(AuthenticationFlowModel flowModel, Requirement requirement, Consumer<FlowUtil> flowInitializer) {
        return addSubFlowExecution(flowModel, requirement, maxPriority + 10, flowInitializer);
    }

    public FlowUtil addSubFlowExecution(AuthenticationFlowModel flowModel, Requirement requirement, int priority, Consumer<FlowUtil> flowInitializer) {
        maxPriority = Math.max(maxPriority, priority);

        flowModel = realm.addAuthenticationFlow(flowModel);

        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setRequirement(requirement);
        execution.setAuthenticatorFlow(true);
        execution.setPriority(priority);
        execution.setFlowId(flowModel.getId());
        execution.setParentFlow(currentFlow.getId());
        realm.addAuthenticatorExecution(execution);

        if (flowInitializer != null) {
            FlowUtil subflow = newFlowUtil(flowModel);
            flowInitializer.accept(subflow);
        }

        return this;
    }

    private List<AuthenticationExecutionModel> getExecutions() {
        if (executions == null) {
            List<AuthenticationExecutionModel> execs = realm.getAuthenticationExecutions(currentFlow.getId());
            if (execs == null) {
                throw new FlowUtilException("Can't get executions of unknown flow " + currentFlow.getId());
            }
            executions = new ArrayList<>(execs);
        }
        return executions;
    }

    public FlowUtil removeExecution(int index) {
        List<AuthenticationExecutionModel> executions = getExecutions();
        realm.removeAuthenticatorExecution(executions.remove(index));

        return this;
    }

    public FlowUtil updateExecution(int index, Consumer<AuthenticationExecutionModel> updater) {
        List<AuthenticationExecutionModel> executions = getExecutions();
        if (executions != null && updater != null) {
            AuthenticationExecutionModel execution = executions.get(index);
            updater.accept(execution);
            realm.updateAuthenticatorExecution(execution);
        }

        return this;
    }
}