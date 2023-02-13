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
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.w3c.dom.Document;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JaxrsSAML2BindingBuilder extends BaseSAML2BindingBuilder<JaxrsSAML2BindingBuilder> {

    private final KeycloakSession session;

    public JaxrsSAML2BindingBuilder(KeycloakSession session) {
        this.session = session;
    }

    public class PostBindingBuilder extends BasePostBindingBuilder {
        public PostBindingBuilder(JaxrsSAML2BindingBuilder builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response request(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return createResponse(actionUrl, GeneralConstants.SAML_REQUEST_KEY);
        }

        public Response response(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return createResponse(actionUrl, GeneralConstants.SAML_RESPONSE_KEY);
        }

        private Response createResponse(String actionUrl, String key) throws ProcessingException, ConfigurationException, IOException {
            MultivaluedMap<String,String> formData = new MultivaluedHashMap<>();
            formData.add(GeneralConstants.URL, actionUrl);
            formData.add(key, BaseSAML2BindingBuilder.getSAMLResponse(document));

            if (this.getRelayState() != null) {
                formData.add(GeneralConstants.RELAY_STATE, this.getRelayState());
            }

            return session.getProvider(LoginFormsProvider.class).setFormData(formData).createSamlPostForm();
        }
    }

    public static class RedirectBindingBuilder extends BaseRedirectBindingBuilder {
        public RedirectBindingBuilder(JaxrsSAML2BindingBuilder builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response response(String redirectUri) throws ProcessingException, ConfigurationException, IOException {
            return response(redirectUri, false);
        }

        public Response request(String redirect) throws ProcessingException, ConfigurationException, IOException {
            return response(redirect, true);
        }

        private Response response(String redirectUri, boolean asRequest) throws ProcessingException, ConfigurationException, IOException {
            URI uri = generateURI(redirectUri, asRequest);
            logger.tracef("redirect-binding uri: %s", uri);
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoCache(true);
            return Response.status(302).location(uri)
                    .header("Pragma", "no-cache")
                    .header("Cache-Control", "no-cache, no-store").build();
        }

    }

    @Override
    public RedirectBindingBuilder redirectBinding(Document document) throws ProcessingException  {
        return new RedirectBindingBuilder(this, document);
    }

    @Override
    public PostBindingBuilder postBinding(Document document) throws ProcessingException  {
        return new PostBindingBuilder(this, document);
    }




}
