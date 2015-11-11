package org.keycloak.authentication.authenticators.broker;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.messages.Messages;

/**
 * Same like classic username+password form, but username is "known" and user can't change it
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpUsernamePasswordForm extends UsernamePasswordForm {

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        UserModel existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getClientSession());

        return setupForm(context, formData, existingUser)
                .setStatus(Response.Status.OK)
                .createLogin();
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        UserModel existingUser = AbstractIdpAuthenticator.getExistingUser(context.getSession(), context.getRealm(), context.getClientSession());
        context.setUser(existingUser);

        // Restore formData for the case of error
        setupForm(context, formData, existingUser);

        return validatePassword(context, formData);
    }

    protected LoginFormsProvider setupForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData, UserModel existingUser) {
        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromClientSession(context.getClientSession());
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }

        formData.add(AuthenticationManager.FORM_USERNAME, existingUser.getUsername());
        return context.form()
                .setFormData(formData)
                .setAttribute(LoginFormsProvider.USERNAME_EDIT_DISABLED, true)
                .setSuccess(Messages.FEDERATED_IDENTITY_CONFIRM_REAUTHENTICATE_MESSAGE, existingUser.getUsername(), serializedCtx.getIdentityProviderId());
    }
}
