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

package org.keycloak.protocol.saml;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil;
import org.w3c.dom.Document;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

public class SOAPOverHTTPBindingBuilder extends JaxrsSAML2BindingBuilder {
    public SOAPOverHTTPBindingBuilder(KeycloakSession session) {
        super(session);
    }

    public class SOAPBindingBuilder extends JaxrsSAML2BindingBuilder.PostBindingBuilder {
        public SOAPBindingBuilder(SOAPOverHTTPBindingBuilder builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response request(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return createResponse(actionUrl, GeneralConstants.SAML_REQUEST_KEY);
        }

        public Response response(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return createResponse(actionUrl, GeneralConstants.SAML_RESPONSE_KEY);
        }

        private Response createResponse(String actionUrl, String key) throws ProcessingException, ConfigurationException, IOException {
            logger.debug("Creating SOAP response");

            Soap.SoapMessageBuilder soapMessageBuilder = Soap.createMessage();
            soapMessageBuilder.addToBody(document);

            if (logger.isTraceEnabled()) {
                String samlResponse = DocumentUtil.asString(document);
                logger.tracev("Sending SAML response message: \n {0}", samlResponse);
            }

            return soapMessageBuilder.build();
        }
    }

    @Override
    public SOAPBindingBuilder postBinding(Document document) throws ProcessingException  {
        return new SOAPBindingBuilder(this, document);
    }
}
