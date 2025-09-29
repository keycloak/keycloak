package org.keycloak.testsuite.util;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AuthenticationManagementResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.keycloak.models.utils.DefaultAuthenticationFlows.BROWSER_FLOW;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.DIRECT_GRANT_FLOW;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.REGISTRATION_FLOW;
import static org.keycloak.models.utils.DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW;

public class FlowUtil {
    private final KeycloakSession session;
    private final RealmModel realm;
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

    private FlowUtil(KeycloakSession session, RealmModel realm) {
        this.session = session;
        this.realm = realm;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public AuthenticationFlowModel build() {
        return currentFlow;
    }

    public static FlowUtil inCurrentRealm(KeycloakSession session) {
        return new FlowUtil(session, session.getContext().getRealm());
    }

    private FlowUtil newFlowUtil(AuthenticationFlowModel flowModel) {
        FlowUtil subflow = new FlowUtil(session, realm);
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
        checkAndRestoreDefaultFlow(realm::getBrowserFlow, realm::setBrowserFlow, newFlowAlias, BROWSER_FLOW);
        return copyFlow(BROWSER_FLOW, newFlowAlias);
    }

    public FlowUtil copyResetCredentialsFlow(String newFlowAlias) {
        checkAndRestoreDefaultFlow(realm::getResetCredentialsFlow, realm::setResetCredentialsFlow, newFlowAlias, RESET_CREDENTIALS_FLOW);
        return copyFlow(RESET_CREDENTIALS_FLOW, newFlowAlias);
    }

    public FlowUtil copyFirstBrokerLoginFlow(String newFlowAlias) {
        return copyFlow(FIRST_BROKER_LOGIN_FLOW, newFlowAlias);
    }

    public FlowUtil copyRegistrationFlow(String newFlowAlias) {
        checkAndRestoreDefaultFlow(realm::getRegistrationFlow, realm::setRegistrationFlow, newFlowAlias, REGISTRATION_FLOW);
        return copyFlow(REGISTRATION_FLOW, newFlowAlias);
    }

    public FlowUtil copyDirectGrantFlow(String newFlowAlias) {
        checkAndRestoreDefaultFlow(realm::getDirectGrantFlow, realm::setDirectGrantFlow, newFlowAlias, DIRECT_GRANT_FLOW);
        return copyFlow(DIRECT_GRANT_FLOW, newFlowAlias);
    }

    public FlowUtil copyFlow(String original, String newFlowAlias) {
        flowAlias = newFlowAlias;
        AuthenticationFlowModel existingBrowserFlow = realm.getFlowByAlias(original);
        if (existingBrowserFlow == null) {
            throw new FlowUtilException("Can't copy flow: " + original + " does not exist");
        }

        // remove new authentication flow with 'newFlowAlias' alias if present
        AuthenticationFlowModel foundFlow = realm.getFlowByAlias(newFlowAlias);
        if (foundFlow != null) {
            clearAuthenticationFlow(foundFlow.getId());
            realm.removeAuthenticationFlow(foundFlow);
        }

        currentFlow = AuthenticationManagementResource.copyFlow(session, realm, existingBrowserFlow, newFlowAlias);

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
        clearAuthenticationFlow(currentFlow.getId());
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
    public FlowUtil defineAsRegistrationFlow() {
        realm.setRegistrationFlow(currentFlow);
        return this;
    }

    public FlowUtil usesInIdentityProvider(String idpAlias) {
        // Setup new FirstBrokerLogin flow to identity provider
        IdentityProviderModel idp = session.identityProviders().getByAlias(idpAlias);
        idp.setFirstBrokerLoginFlowId(currentFlow.getId());
        session.identityProviders().update(idp);
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
        //KEYCLOAK-14161
        if (flowModel.getProviderId() == "form-flow") {
            execution.setAuthenticator("registration-page-form");
        }
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

    public List<AuthenticationExecutionModel> getExecutions() {
        if (executions == null) {
            executions = realm.getAuthenticationExecutionsStream(currentFlow.getId()).collect(Collectors.toList());
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

    /**
     * Remove authentication flows and executions included in the specified flow
     *
     * @param flowId id of flow, which content will be removed
     */
    private void clearAuthenticationFlow(String flowId) {
        realm.getAuthenticationExecutionsStream(flowId)
                .filter(Objects::nonNull)
                .forEachOrdered(f -> {
                    if (f.isAuthenticatorFlow() && f.getFlowId() != null) {
                        clearAuthenticationFlow(f.getFlowId());
                        realm.removeAuthenticationFlow(realm.getAuthenticationFlowById(f.getFlowId()));
                    }
                    if (f.getAuthenticatorConfig() != null) {
                        realm.removeAuthenticatorConfig(realm.getAuthenticatorConfigById(f.getAuthenticatorConfig()));
                    }
                    realm.removeAuthenticatorExecution(f);
                });
    }

    /**
     * Check whether the new flow is set as default one
     * If yes, restore the default one
     * <p>
     * Usable for removing flow, which must not be used as the default flow
     *
     * @param getFlow          getter for obtaining the default flow
     * @param setFlow          setter for the setting of the default flow
     * @param newFlowAlias     alias of tested flow
     * @param defaultFlowAlias default flow alias
     */
    private void checkAndRestoreDefaultFlow(Supplier<AuthenticationFlowModel> getFlow,
                                            Consumer<AuthenticationFlowModel> setFlow,
                                            String newFlowAlias,
                                            String defaultFlowAlias) {
        if (getFlow == null || setFlow == null || newFlowAlias == null || defaultFlowAlias == null) return;

        final String alias = Optional.ofNullable(getFlow.get())
                .map(AuthenticationFlowModel::getAlias)
                .orElse(null);

        if (alias != null && alias.equals(newFlowAlias)) {
            setFlow.accept(realm.getFlowByAlias(defaultFlowAlias));
        }
    }

    /**
     * <p>Sets the given {@code key} and {@code value} to an execution that maps to the given {@code authenticatorId}.
     *
     * <p>This method will try to find the given {@code authenticatorId} recursively by going through all the subflows, if there are any.
     *
     * @param session the session
     * @param flowId the parent flow
     * @param authenticatorId the authenticator id
     * @param key the key
     * @param value the value
     */
    public static void setAuthenticatorConfig(KeycloakSession session, String flowId, String authenticatorId, String key, String value) {
        RealmModel realm = session.getContext().getRealm();

        for (AuthenticationExecutionModel execution : Optional.ofNullable(realm.getAuthenticationExecutionsStream(flowId)).orElse(Stream.empty()).toList()) {
            if (execution.isAuthenticatorFlow()) {
                setAuthenticatorConfig(session, execution.getFlowId(), authenticatorId, key, value);
            } else if (authenticatorId.equals(execution.getAuthenticator())) {
                AuthenticatorConfigModel configModel;
                String configId = execution.getAuthenticatorConfig();

                if (configId == null) {
                    configModel = new AuthenticatorConfigModel();
                    configModel.setAlias(authenticatorId + flowId);
                    configModel = realm.addAuthenticatorConfig(configModel);
                    execution.setAuthenticatorConfig(configModel.getId());
                    realm.updateAuthenticatorExecution(execution);
                } else {
                    configModel = realm.getAuthenticatorConfigById(configId);
                }

                Map<String, String> config = new HashMap<>(configModel.getConfig());

                configModel.setConfig(config);
                config.put(key, value);
                configModel.setConfig(config);

                realm.updateAuthenticatorConfig(configModel);
            }
        }
    }
}
