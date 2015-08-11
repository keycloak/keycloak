package org.keycloak.authentication;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.services.resources.LoginActionsService;

import java.net.URI;

/**
 * Abstract helper class that Authenticator implementations can leverage
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractFormRequiredAction implements RequiredActionProvider {

    /**
     * Get the action URL for the required action.
     *
     * @param context
     * @param code client sessino access code
     * @return
     */
    public URI getActionUrl(RequiredActionContext context, String code) {
        return LoginActionsService.requiredActionProcessor(context.getUriInfo())
                .queryParam(OAuth2Constants.CODE, code)
                .queryParam("action", getProviderId())
                .build(context.getRealm().getName());
    }

    /**
     * Get the action URL for the required action.  This auto-generates the access code.
     *
     * @param context
     * @return
     */
    public URI getActionUrl(RequiredActionContext context) {
        String accessCode = context.generateAccessCode(getProviderId());
        return getActionUrl(context, accessCode);

    }

    /**
     * Create a form builder that presets the user, action URI, and a generated access code
     *
     * @param context
     * @return
     */
    public LoginFormsProvider form(RequiredActionContext context) {
        String accessCode = context.generateAccessCode(getProviderId());
        URI action = getActionUrl(context, accessCode);
        LoginFormsProvider provider = context.getSession().getProvider(LoginFormsProvider.class)
                .setUser(context.getUser())
                .setActionUri(action)
                .setClientSessionCode(accessCode);
        return provider;
    }

    @Override
    public void close() {

    }


}
