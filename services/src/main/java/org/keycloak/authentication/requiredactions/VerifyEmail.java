package org.keycloak.authentication.requiredactions;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
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
        if (Validation.isBlank(context.getUser().getEmail())) {
            return null;
        }

        context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, context.getUser().getEmail()).success();
        LoginActionsService.createActionCookie(context.getRealm(), context.getUriInfo(), context.getConnection(), context.getUserSession().getId());

        LoginFormsProvider loginFormsProvider = context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(context.generateAccessCode(getProviderId()))
                .setUser(context.getUser());
        return loginFormsProvider.createResponse(UserModel.RequiredAction.VERIFY_EMAIL);
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
        return "Verify Email";
    }


    @Override
    public String getId() {
        return UserModel.RequiredAction.VERIFY_EMAIL.name();
    }

    @Override
    public String getProviderId() {
        return getId();
    }


}
