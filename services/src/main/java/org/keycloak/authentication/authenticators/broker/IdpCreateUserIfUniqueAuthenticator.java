package org.keycloak.authentication.authenticators.broker;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.util.ExistingUserInfo;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpCreateUserIfUniqueAuthenticator extends AbstractIdpAuthenticator {

    protected static Logger logger = Logger.getLogger(IdpCreateUserIfUniqueAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        if (context.getClientSession().getNote(EXISTING_USER_INFO) != null) {
            context.attempted();
            return;
        }

        String username = getUsername(context, serializedCtx, brokerContext);
        if (username == null) {
            logger.warnf("%s is null. Reset flow and enforce showing reviewProfile page", realm.isRegistrationEmailAsUsername() ? "Email" : "Username");
            context.getClientSession().setNote(ENFORCE_UPDATE_PROFILE, "true");
            context.resetFlow();
            return;
        }

        ExistingUserInfo duplication = checkExistingUser(context, username, serializedCtx, brokerContext);

        if (duplication == null) {
            logger.debugf("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username, brokerContext.getIdpConfig().getAlias());

            UserModel federatedUser = session.users().addUser(realm, username);
            federatedUser.setEnabled(true);
            federatedUser.setEmail(brokerContext.getEmail());
            federatedUser.setFirstName(brokerContext.getFirstName());
            federatedUser.setLastName(brokerContext.getLastName());

            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet()) {
                federatedUser.setAttribute(attr.getKey(), attr.getValue());
            }

            AuthenticatorConfigModel config = context.getAuthenticatorConfig();
            if (config != null && Boolean.parseBoolean(config.getConfig().get(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION))) {
                logger.debugf("User '%s' required to update password", federatedUser.getUsername());
                federatedUser.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
            }

            // TODO: Event

            context.setUser(federatedUser);
            context.getClientSession().setNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else {
            logger.debugf("Duplication detected. There is already existing user with %s '%s' .",
                    duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue());

            // Set duplicated user, so next authenticators can deal with it
            context.getClientSession().setNote(EXISTING_USER_INFO, duplication.serialize());

            Response challengeResponse = context.form()
                    .setError(Messages.FEDERATED_IDENTITY_EXISTS, duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue())
                    .createErrorPage();
            context.challenge(challengeResponse);

            if (context.getExecution().isRequired()) {
                context.getEvent()
                        .user(duplication.getExistingUserId())
                        .detail("existing_" + duplication.getDuplicateAttributeName(), duplication.getDuplicateAttributeValue())
                        .removeDetail(Details.AUTH_METHOD)
                        .removeDetail(Details.AUTH_TYPE)
                        .error(Errors.FEDERATED_IDENTITY_EXISTS);
            }
        }
    }

    // Could be overriden to detect duplication based on other criterias (firstName, lastName, ...)
    protected ExistingUserInfo checkExistingUser(AuthenticationFlowContext context, String username, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

        if (brokerContext.getEmail() != null) {
            UserModel existingUser = context.getSession().users().getUserByEmail(brokerContext.getEmail(), context.getRealm());
            if (existingUser != null) {
                return new ExistingUserInfo(existingUser.getId(), UserModel.EMAIL, existingUser.getEmail());
            }
        }

        UserModel existingUser = context.getSession().users().getUserByUsername(username, context.getRealm());
        if (existingUser != null) {
            return new ExistingUserInfo(existingUser.getId(), UserModel.USERNAME, existingUser.getUsername());
        }

        return null;
    }

    protected String getUsername(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        RealmModel realm = context.getRealm();
        return realm.isRegistrationEmailAsUsername() ? brokerContext.getEmail() : brokerContext.getModelUsername();
    }


    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
