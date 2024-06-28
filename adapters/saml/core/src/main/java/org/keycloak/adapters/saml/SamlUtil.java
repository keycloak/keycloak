/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.saml;

import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.util.XMLTimeUtil;
import org.w3c.dom.Document;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlUtil {

    protected static Logger log = Logger.getLogger(SamlUtil.class);

    public static void sendSaml(boolean asRequest, HttpFacade httpFacade, String actionUrl,
                            BaseSAML2BindingBuilder binding, Document document,
                            SamlDeployment.Binding samlBinding) throws ProcessingException, ConfigurationException, IOException {
        if (samlBinding == SamlDeployment.Binding.POST) {
            String html = asRequest ? binding.postBinding(document).getHtmlRequest(actionUrl) : binding.postBinding(document).getHtmlResponse(actionUrl) ;
            httpFacade.getResponse().setStatus(200);
            httpFacade.getResponse().setHeader("Content-Type", "text/html");
            httpFacade.getResponse().setHeader("Pragma", "no-cache");
            httpFacade.getResponse().setHeader("Cache-Control", "no-cache, no-store");
            httpFacade.getResponse().getOutputStream().write(html.getBytes(GeneralConstants.SAML_CHARSET));
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

    public static SamlSession validateSamlSession(Object potentialSamlSession, SamlDeployment deployment) {
        if (potentialSamlSession == null) {
            log.debug("SamlSession was not found in the session");
            return null;
        }

        if (!(potentialSamlSession instanceof SamlSession)) {
            log.debug("Provided samlSession was not SamlSession type");
            return null;
        }

        SamlSession samlSession = (SamlSession) potentialSamlSession;

        XMLGregorianCalendar sessionNotOnOrAfter = samlSession.getSessionNotOnOrAfter();
        if (sessionNotOnOrAfter != null) {
            XMLGregorianCalendar now = XMLTimeUtil.getIssueInstant();

            XMLTimeUtil.add(sessionNotOnOrAfter, deployment.getIDP().getAllowedClockSkew()); // add clockSkew

            if (now.compare(sessionNotOnOrAfter) != DatatypeConstants.LESSER) {
                return null;
            }
        }

        return samlSession;
    }
}
