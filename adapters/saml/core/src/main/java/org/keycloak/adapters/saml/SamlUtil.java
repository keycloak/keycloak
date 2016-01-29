package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlUtil {
    public static void sendSaml(boolean asRequest, HttpFacade httpFacade, String actionUrl,
                            BaseSAML2BindingBuilder binding, Document document,
                            SamlDeployment.Binding samlBinding) throws ProcessingException, ConfigurationException, IOException {
        if (samlBinding == SamlDeployment.Binding.POST) {
            String html = asRequest ? binding.postBinding(document).getHtmlRequest(actionUrl) : binding.postBinding(document).getHtmlResponse(actionUrl) ;
            httpFacade.getResponse().setStatus(200);
            httpFacade.getResponse().setHeader("Content-Type", "text/html");
            httpFacade.getResponse().setHeader("Pragma", "no-cache");
            httpFacade.getResponse().setHeader("Cache-Control", "no-cache, no-store");
            httpFacade.getResponse().getOutputStream().write(html.getBytes());
            httpFacade.getResponse().end();
        } else {
            String uri = asRequest ? binding.redirectBinding(document).requestURI(actionUrl).toString() : binding.redirectBinding(document).responseURI(actionUrl).toString();
            httpFacade.getResponse().setStatus(302);
            httpFacade.getResponse().setHeader("Location", uri);
            httpFacade.getResponse().end();
        }
    }

    /**
     * Gets a url to redirect to if there is an IDP initiated login.  Looks for a redirectTo query param first, then looks
     * in RelayState, if not in either defaults to context path.
     *
     * @param facade
     * @param contextPath
     * @param baseUri
     * @return
     */
    public static String getRedirectTo(HttpFacade facade, String contextPath, String baseUri) {
        String redirectTo = facade.getRequest().getQueryParamValue("redirectTo");
        if (redirectTo != null && !redirectTo.isEmpty()) {
            return buildRedirectTo(baseUri, redirectTo);
        } else {
            redirectTo = facade.getRequest().getFirstParam(GeneralConstants.RELAY_STATE);
            if (redirectTo != null) {
                int index = redirectTo.indexOf("redirectTo=");
                if (index >= 0) {
                    String to = redirectTo.substring(index + "redirectTo=".length());
                    index = to.indexOf(';');
                    if (index >=0) {
                        to = to.substring(0, index);
                    }
                    return buildRedirectTo(baseUri, to);
                }
            }
            if (contextPath.isEmpty()) baseUri += "/";
            return baseUri;
        }
    }

    private static String buildRedirectTo(String baseUri, String redirectTo) {
        if (redirectTo.startsWith("/")) redirectTo = redirectTo.substring(1);
        if (baseUri.endsWith("/")) baseUri = baseUri.substring(0, baseUri.length() - 1);
        redirectTo = baseUri + "/" + redirectTo;
        return redirectTo;
    }
}
