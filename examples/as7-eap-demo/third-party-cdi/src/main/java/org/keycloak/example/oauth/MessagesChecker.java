package org.keycloak.example.oauth;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * This is needed because Faces context is not available in HTTP filters
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@RequestScoped
@Named("messagesChecker")
public class MessagesChecker {

    @Inject
    @ServletRequestQualifier
    private HttpServletRequest request;

    @Inject
    private FacesContext facesContext;

    public String getCheckMessage() {
        String oauthError = (String)request.getAttribute(RefreshTokenFilter.OAUTH_ERROR_ATTR);
        if (oauthError != null) {
            facesContext.addMessage(null, new FacesMessage("OAuth error occured: " + oauthError));
        }

        return null;
    }
}
