package org.keycloak.authentication.authenticators;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.constants.KerberosConstants;
import org.keycloak.events.Errors;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SpnegoAuthenticator extends AbstractFormAuthenticator implements Authenticator{
    protected static Logger logger = Logger.getLogger(SpnegoAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return false;
    }

    protected boolean isAlreadyChallenged(AuthenticatorContext context) {
        UserSessionModel.AuthenticatorStatus status = context.getClientSession().getAuthenticators().get(context.getExecution().getId());
        if (status == null) return false;
        return status == UserSessionModel.AuthenticatorStatus.CHALLENGED;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        HttpRequest request = context.getHttpRequest();
        String authHeader = request.getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Case when we don't yet have any Negotiate header
        if (authHeader == null) {
            if (isAlreadyChallenged(context)) {
                context.attempted();
                return;
            }
            Response challenge = challengeNegotiation(context, null);
            context.challenge(challenge);
            return;
        }

        String[] tokens = authHeader.split(" ");
        if (tokens.length == 0) { // assume not supported
            logger.debug("Invalid length of tokens: " + tokens.length);
            context.attempted();
            return;
        }
        if (!KerberosConstants.NEGOTIATE.equalsIgnoreCase(tokens[0])) {
            logger.debug("Unknown scheme " + tokens[0]);
            context.attempted();
            return;
        }
        if (tokens.length != 2) {
            context.failure(AuthenticationProcessor.Error.INVALID_CREDENTIALS);
            return;
        }

        String spnegoToken = tokens[1];
        UserCredentialModel spnegoCredential = UserCredentialModel.kerberos(spnegoToken);

        CredentialValidationOutput output = context.getSession().users().validCredentials(context.getRealm(), spnegoCredential);

        if (output.getAuthStatus() == CredentialValidationOutput.Status.AUTHENTICATED) {
            context.setUser(output.getAuthenticatedUser());
            if (output.getState() != null && !output.getState().isEmpty()) {
                for (Map.Entry<String, String> entry : output.getState().entrySet()) {
                    context.getClientSession().setUserSessionNote(entry.getKey(), entry.getValue());
                }
            }
            context.success();
        } else if (output.getAuthStatus() == CredentialValidationOutput.Status.CONTINUE) {
            String spnegoResponseToken = (String) output.getState().get(KerberosConstants.RESPONSE_TOKEN);
            Response challenge =  challengeNegotiation(context, spnegoResponseToken);
            context.challenge(challenge);
        } else {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            context.failure(AuthenticationProcessor.Error.INVALID_CREDENTIALS);
        }
    }

    private Response challengeNegotiation(AuthenticatorContext context, final String negotiateToken) {
        String negotiateHeader = negotiateToken == null ? KerberosConstants.NEGOTIATE : KerberosConstants.NEGOTIATE + " " + negotiateToken;

        if (logger.isTraceEnabled()) {
            logger.trace("Sending back " + HttpHeaders.WWW_AUTHENTICATE + ": " + negotiateHeader);
        }
        LoginFormsProvider loginForm = loginForm(context);

        loginForm.setStatus(Response.Status.UNAUTHORIZED);
        loginForm.setResponseHeader(HttpHeaders.WWW_AUTHENTICATE, negotiateHeader);
        return loginForm.createLogin();
    }


    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public String getRequiredAction() {
        return null;
    }

    @Override
    public void close() {

    }
}
