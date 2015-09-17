package org.keycloak.adapters.saml;

import org.keycloak.adapters.HttpFacade;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlUtil {
    public static void sendSaml(HttpFacade httpFacade, String actionUrl, BaseSAML2BindingBuilder binding, Document document, SamlDeployment.Binding samlBinding) throws ProcessingException, ConfigurationException, IOException {
        if (samlBinding == SamlDeployment.Binding.POST) {
            String html = binding.postBinding(document).getHtmlRequest(actionUrl);
            httpFacade.getResponse().setStatus(200);
            httpFacade.getResponse().setHeader("Content-Type", "text/html");
            httpFacade.getResponse().setHeader("Pragma", "no-cache");
            httpFacade.getResponse().setHeader("Cache-Control", "no-cache, no-store");
            httpFacade.getResponse().getOutputStream().write(html.getBytes());
            httpFacade.getResponse().end();
        } else {
            String uri = binding.redirectBinding(document).requestURI(actionUrl).toString();
            httpFacade.getResponse().setStatus(302);
            httpFacade.getResponse().setHeader("Location", uri);
        }
    }
}
