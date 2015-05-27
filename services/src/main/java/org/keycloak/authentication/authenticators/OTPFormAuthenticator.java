package org.keycloak.authentication.authenticators;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorContext;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OTPFormAuthenticator implements Authenticator {
    protected AuthenticatorModel model;

    public OTPFormAuthenticator(AuthenticatorModel model) {
        this.model = model;
    }

    @Override
    public void authenticate(AuthenticatorContext context) {
        URI expected = LoginActionsService.authenticationFormProcessor(context.getUriInfo()).build(context.getRealm().getName());
        if (!expected.getPath().equals(context.getUriInfo().getPath())) {
            Response challengeResponse = challenge(context);
            context.challenge(challengeResponse);
            return;
        }
        validateOTP(context);
    }

    public void validateOTP(AuthenticatorContext context) {
        MultivaluedMap<String, String> inputData = context.getHttpRequest().getFormParameters();
        List<UserCredentialModel> credentials = new LinkedList<>();
        String password = inputData.getFirst(CredentialRepresentation.TOTP);
        if (password == null) {
            Response challengeResponse = challenge(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return;
        }
        credentials.add(UserCredentialModel.totp(password));
        boolean valid = context.getSession().users().validCredentials(context.getRealm(), context.getUser(), credentials);
        if (!valid) {
            Response challengeResponse = challenge(context);
            context.failureChallenge(AuthenticationProcessor.Error.INVALID_CREDENTIALS, challengeResponse);
            return;
        }
        context.success();
    }


    @Override
    public boolean requiresUser() {
        return true;
    }

    protected Response challenge(AuthenticatorContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.getSession().getProvider(LoginFormsProvider.class)
                .setClientSessionCode(new ClientSessionCode(context.getRealm(), context.getClientSession()).getCode());

        if (formData.size() > 0) forms.setFormData(formData);

        return forms.createLoginTotp();
    }

    public Response challenge(AuthenticatorContext context) {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<>();
        return challenge(context, formData);
    }

    @Override
    public boolean configuredFor(UserModel user) {
        return user.configuredForCredentialType(UserCredentialModel.TOTP);
    }

    @Override
    public String getRequiredAction() {
        return UserModel.RequiredAction.CONFIGURE_TOTP.name();
    }

    @Override
    public void close() {

    }
}
