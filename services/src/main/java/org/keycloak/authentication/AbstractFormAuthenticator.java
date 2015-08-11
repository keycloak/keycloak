package org.keycloak.authentication;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.services.resources.LoginActionsService;

import java.net.URI;

/**
 * Abstract helper class that Authenticator implementations can leverage
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractFormAuthenticator implements Authenticator {
    public static final String EXECUTION = "execution";

    @Override
    public void close() {

    }

    /**
     * Create a form builder that presets the user, action URI, and a generated access code
     *
     * @param context
     * @return
     */
    protected LoginFormsProvider loginForm(AuthenticationFlowContext context) {
        String accessCode = context.generateAccessCode();
        URI action = getActionUrl(context, accessCode);
        LoginFormsProvider provider = context.getSession().getProvider(LoginFormsProvider.class)
                    .setUser(context.getUser())
                    .setActionUri(action)
                    .setClientSessionCode(accessCode);
        if (context.getForwardedErrorMessage() != null) {
            provider.setError(context.getForwardedErrorMessage());
        }
        return provider;
    }

    /**
     * Get the action URL for the required action.
     *
     * @param context
     * @param code client sessino access code
     * @return
     */
    public URI getActionUrl(AuthenticationFlowContext context, String code) {
        return LoginActionsService.authenticationFormProcessor(context.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code)
                .queryParam(EXECUTION, context.getExecution().getId())
                .build(context.getRealm().getName());
    }

    /**
     * Get the action URL for the required action.  This auto-generates the access code.
     *
     * @param context
     * @return
     */
    public URI getActionUrl(AuthenticationFlowContext context) {
        return getActionUrl(context, context.generateAccessCode());
    }
}
