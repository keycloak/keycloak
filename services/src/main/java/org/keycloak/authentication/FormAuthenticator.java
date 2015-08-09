package org.keycloak.authentication;

import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * This class is responsible for rendering a form.  The way it works is that each FormAction that is a child of this
 * FormAuthenticator, will have its buildPage() method call first, then the FormAuthenticator.render() method will be invoked.
 *
 * This gives each FormAction a chance to add information to the form in an independent manner.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormAuthenticator extends Provider {
    /**
     * Called to render the FormAuthenticator's challenge page.  If null is returned, then success is assumed and the
     * next authenticator in the flow will be invoked.
     *
     * @param context
     * @param form
     * @return
     */
    Response render(FormContext context, LoginFormsProvider form);
}
