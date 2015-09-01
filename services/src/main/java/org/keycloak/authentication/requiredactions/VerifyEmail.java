package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.Time;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VerifyEmail implements RequiredActionProvider, RequiredActionFactory {
    protected static Logger logger = Logger.getLogger(VerifyEmail.class);
    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            logger.debug("User is required to verify email");
        }
    }
    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        // if this is EXECUTE_ACTIONS we know that the email sent is valid so we can verify it automatically
        if (context.getClientSession().getNote(ClientSessionModel.Action.EXECUTE_ACTIONS.name()) != null) {
            context.getUser().setEmailVerified(true);
            context.success();
            return;
        }

        if (Validation.isBlank(context.getUser().getEmail())) {
            context.ignore();
            return;
        }

        context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, context.getUser().getEmail()).success();
        LoginActionsService.createActionCookie(context.getRealm(), context.getUriInfo(), context.getConnection(), context.getUserSession().getId());

        LoginFormsProvider loginFormsProvider = context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(context.generateAccessCode(UserModel.RequiredAction.VERIFY_EMAIL.name()))
                .setUser(context.getUser());
        Response challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        context.failure();
    }


    @Override
    public void close() {

    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public String getDisplayText() {
        return "Verify Email";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.VERIFY_EMAIL.name();
    }
}
