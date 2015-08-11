package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.CredentialRepresentation;

import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdateTotp implements RequiredActionProvider, RequiredActionFactory {
    protected static Logger logger = Logger.getLogger(UpdateTotp.class);
    @Override
    public void evaluateTriggers(RequiredActionContext context) {
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
         LoginFormsProvider loginFormsProvider = context.getSession().getProvider(LoginFormsProvider.class)
                 .setClientSessionCode(context.generateAccessCode(getProviderId()))
                .setUser(context.getUser());
        Response challenge = loginFormsProvider.createResponse(UserModel.RequiredAction.CONFIGURE_TOTP);
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
        return "Configure Totp";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.CONFIGURE_TOTP.name();
    }

    @Override
    public String getProviderId() {
        return getId();
    }


}
