package org.keycloak.authentication.authenticators.browser;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.AuthenticationFlowResolver;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MfaEnrollmentAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(MfaEnrollmentAuthenticator.class);

    private static final String TEMPLATE_NAME = "mfa-enrollment.ftl";
    private static final String FORM_PARAM_MFA_METHOD = "mfaMethod";

    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        MfaEnrollmentConfig mfaEnrollmentConfig = new MfaEnrollmentConfig(
                authenticationFlowContext.getAuthenticatorConfig());
        List<String> configuredRequiredActions = mfaEnrollmentConfig.getRequiredActions();

        List<RequiredActionProviderModel> requiredActions = getRequiredActions(authenticationFlowContext);

        if (requiredActions.isEmpty()) {
            LOG.warnf("No supported required actions enabled for authenticator %s in realm %s.",
                    authenticationFlowContext.getFlowPath(), authenticationFlowContext.getRealm().getName());
            authenticationFlowContext.attempted();
        }

        authenticationFlowContext.challenge(
                authenticationFlowContext
                        .form()
                        .setAttribute("mfa", new MfaEnrollmentBean(requiredActions))
                        .createForm(TEMPLATE_NAME)
        );
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
        MultivaluedMap<String, String> decodedFormParameters = authenticationFlowContext.getHttpRequest().getDecodedFormParameters();

        if (!decodedFormParameters.containsKey(FORM_PARAM_MFA_METHOD)) {
            authenticationFlowContext.challenge(
                    authenticationFlowContext.form().createErrorPage(Response.Status.BAD_REQUEST));
            authenticationFlowContext.failure(AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED);
            return;
        }

        String action = decodedFormParameters.getFirst(FORM_PARAM_MFA_METHOD);
        Stream<String> requiredActions = getRequiredActions(authenticationFlowContext).stream()
                .map(RequiredActionProviderModel::getProviderId);
        if (requiredActions.noneMatch(it -> it.equals(action))) {
            authenticationFlowContext.challenge(
                    authenticationFlowContext.form().createErrorPage(Response.Status.BAD_REQUEST));
            authenticationFlowContext.failure(AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED);
            return;
        }

        AuthenticationSessionModel authenticationSession = authenticationFlowContext.getAuthenticationSession();
        if (!authenticationSession.getRequiredActions().contains(action)) {
            authenticationSession.addRequiredAction(action);
        }
        authenticationFlowContext.success();
    }

    private static List<RequiredActionProviderModel> getRequiredActions(AuthenticationFlowContext authenticationFlowContext) {
        MfaEnrollmentConfig mfaEnrollmentConfig = new MfaEnrollmentConfig(
                authenticationFlowContext.getAuthenticatorConfig());
        List<String> configuredRequiredActions = mfaEnrollmentConfig.getRequiredActions();
        return authenticationFlowContext.getRealm().getRequiredActionProvidersStream()
                .filter(RequiredActionProviderModel::isEnabled)
                .filter(it -> configuredRequiredActions.contains(it.getProviderId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        AuthenticationFlowModel browserFlow = AuthenticationFlowResolver.resolveBrowserFlow(
                keycloakSession.getContext().getAuthenticationSession()
        );
        List<AuthenticationExecutionModel> executions = realmModel
                .getAuthenticationExecutionsStream(browserFlow.getId())
                .collect(Collectors.toList());
        MfaEnrollmentConfig config = null;
        for (int i = 0; i < executions.size(); i++) {
            AuthenticationExecutionModel execution = executions.get(i);
            if (execution.isAuthenticatorFlow()) {
                executions.addAll(realmModel
                        .getAuthenticationExecutionsStream(execution.getFlowId())
                        .collect(Collectors.toList()));
            } else {
                if (MfaEnrollmentAuthenticatorFactory.PROVIDER_ID.equals(execution.getAuthenticator())) {
                    String configId = execution.getAuthenticatorConfig();
                    AuthenticatorConfigModel authenticatorConfig = realmModel.getAuthenticatorConfigById(configId);
                    config = new MfaEnrollmentConfig(authenticatorConfig);
                    break;
                }
            }
        }
        if (config == null) {
            config = new MfaEnrollmentConfig(null);
        }

        List<String> credentialTypes = config.getCredentialTypes();
        return credentialTypes.stream().noneMatch(
                credentialType -> userModel.credentialManager().isConfiguredFor(credentialType));
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }
}
