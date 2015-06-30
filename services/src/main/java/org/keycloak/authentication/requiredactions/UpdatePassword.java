package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.util.Time;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UpdatePassword implements RequiredActionProvider, RequiredActionFactory {
    protected static Logger logger = Logger.getLogger(UpdatePassword.class);
    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        int daysToExpirePassword = context.getRealm().getPasswordPolicy().getDaysToExpirePassword();
        if(daysToExpirePassword != -1) {
            for (UserCredentialValueModel entity : context.getUser().getCredentialsDirectly()) {
                if (entity.getType().equals(UserCredentialModel.PASSWORD)) {

                    if(entity.getCreatedDate() == null) {
                        context.getUser().addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                        logger.debug("User is required to update password");
                    } else {
                        long timeElapsed = Time.toMillis(Time.currentTime()) - entity.getCreatedDate();
                        long timeToExpire = TimeUnit.DAYS.toMillis(daysToExpirePassword);

                        if(timeElapsed > timeToExpire) {
                            context.getUser().addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
                            logger.debug("User is required to update password");
                        }
                    }
                    break;
                }
            }
        }
    }

    @Override
    public Response invokeRequiredAction(RequiredActionContext context) {
        LoginFormsProvider loginFormsProvider = context.getSession()
                .getProvider(LoginFormsProvider.class)
                .setClientSessionCode(context.generateAccessCode(getProviderId()))
                .setUser(context.getUser());
        return loginFormsProvider.createResponse(UserModel.RequiredAction.UPDATE_PASSWORD);
    }

    @Override
    public Object jaxrsService(RequiredActionContext context) {
        // this is handled by LoginActionsService at the moment
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
        return "Update Password";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.UPDATE_PASSWORD.name();
    }

    @Override
    public String getProviderId() {
        return getId();
    }

}
