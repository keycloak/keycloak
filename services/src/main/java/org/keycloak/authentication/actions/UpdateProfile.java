package org.keycloak.authentication.actions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdateProfile implements RequiredActionProvider, RequiredActionFactory {
    protected static Logger logger = Logger.getLogger(UpdateProfile.class);
    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL);
            logger.debug("User is required to verify email");
        }
    }

    @Override
    public Response invokeRequiredAction(RequiredActionContext context) {
        ClientSessionCode accessCode = new ClientSessionCode(context.getRealm(), context.getClientSession());
        accessCode.setAction(ClientSessionModel.Action.UPDATE_PROFILE.name());

        LoginFormsProvider loginFormsProvider = context.getSession().getProvider(LoginFormsProvider.class).setClientSessionCode(accessCode.getCode())
                .setUser(context.getUser());
        return loginFormsProvider.createResponse(UserModel.RequiredAction.UPDATE_PROFILE);
    }

    @Override
    public Object jaxrsService(RequiredActionContext context) {
        // this is handled by LoginActionsService at the moment
        // todo should be refactored to contain it here
        return null;
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
        return "Update Profile";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.UPDATE_PROFILE.name();
    }
}
