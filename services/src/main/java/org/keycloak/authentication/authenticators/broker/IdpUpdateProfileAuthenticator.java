package org.keycloak.authentication.authenticators.broker;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.services.validation.Validation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpUpdateProfileAuthenticator extends AbstractIdpAuthenticator {

    protected static Logger logger = Logger.getLogger(IdpUpdateProfileAuthenticator.class);

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext userCtx, BrokeredIdentityContext brokerContext) {
        IdentityProviderModel idpConfig = brokerContext.getIdpConfig();

        if (requiresUpdateProfilePage(context, userCtx, brokerContext)) {

            logger.debugf("Identity provider '%s' requires update profile action for broker user '%s'.", idpConfig.getAlias(), userCtx.getUsername());

            // No formData for first render. The profile is rendered from userCtx
            Response challengeResponse = context.form()
                    .setAttribute(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR, userCtx)
                    .setFormData(null)
                    .createUpdateProfilePage();
            context.challenge(challengeResponse);
        } else {
            // Not required to update profile. Marked success
            context.success();
        }
    }

    protected boolean requiresUpdateProfilePage(AuthenticationFlowContext context, SerializedBrokeredIdentityContext userCtx, BrokeredIdentityContext brokerContext) {
        String enforceUpdateProfile = context.getClientSession().getNote(ENFORCE_UPDATE_PROFILE);
        if (Boolean.parseBoolean(enforceUpdateProfile)) {
            return true;
        }

        IdentityProviderModel idpConfig = brokerContext.getIdpConfig();
        RealmModel realm = context.getRealm();
        return IdentityProviderRepresentation.UPFLM_ON.equals(idpConfig.getUpdateProfileFirstLoginMode())
                || (IdentityProviderRepresentation.UPFLM_MISSING.equals(idpConfig.getUpdateProfileFirstLoginMode()) && !Validation.validateUserMandatoryFields(realm, userCtx));
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext userCtx, BrokeredIdentityContext brokerContext) {
        EventBuilder event = context.getEvent();
        event.event(EventType.UPDATE_PROFILE);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        RealmModel realm = context.getRealm();

        List<FormMessage> errors = Validation.validateUpdateProfileForm(true, formData);
        if (errors != null && !errors.isEmpty()) {
            Response challenge = context.form()
                    .setErrors(errors)
                    .setAttribute(LoginFormsProvider.UPDATE_PROFILE_CONTEXT_ATTR, userCtx)
                    .setFormData(formData)
                    .createUpdateProfilePage();
            context.challenge(challenge);
            return;
        }

        userCtx.setUsername(formData.getFirst(UserModel.USERNAME));
        userCtx.setFirstName(formData.getFirst(UserModel.FIRST_NAME));
        userCtx.setLastName(formData.getFirst(UserModel.LAST_NAME));

        String email = formData.getFirst(UserModel.EMAIL);
        if (!ObjectUtil.isEqualOrBothNull(email, userCtx.getEmail())) {
            if (logger.isTraceEnabled()) {
                logger.tracef("Email updated on updateProfile page to '%s' ", email);
            }

            userCtx.setEmail(email);
            context.getClientSession().setNote(UPDATE_PROFILE_EMAIL_CHANGED, "true");
        }

        AttributeFormDataProcessor.process(formData, realm, userCtx);

        userCtx.saveToClientSession(context.getClientSession());

        logger.debugf("Profile updated successfully after first authentication with identity provider '%s' for broker user '%s'.", brokerContext.getIdpConfig().getAlias(), userCtx.getUsername());

        event.detail(Details.UPDATED_EMAIL, email);
        context.success();
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
