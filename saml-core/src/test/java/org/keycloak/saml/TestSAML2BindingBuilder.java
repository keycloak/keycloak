/*
 * JBoss, Home of Professional Open Source
 * Copyright 2026 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.saml;

import java.io.IOException;
import java.net.URI;

import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;

/**
 * SAML2BindingBuilder for testing purpose.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class TestSAML2BindingBuilder extends BaseSAML2BindingBuilder<TestSAML2BindingBuilder> {

    public static class RedirectBindingBuilder extends BaseRedirectBindingBuilder {

        public RedirectBindingBuilder(TestSAML2BindingBuilder builder) throws ProcessingException {
            super(builder, null);
        }

        public String response(String redirectUri) throws ProcessingException, ConfigurationException, IOException {
            return response(redirectUri, false);
        }

        public String request(String redirect) throws ProcessingException, ConfigurationException, IOException {
            return response(redirect, true);
        }

        private String response(String redirectUri, boolean asRequest) throws ProcessingException, ConfigurationException, IOException {
            URI uri = generateURI(redirectUri, asRequest);
            return uri.toString();
        }


    }

}
