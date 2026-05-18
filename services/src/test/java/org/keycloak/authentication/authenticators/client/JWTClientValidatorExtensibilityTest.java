/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.client;

import java.lang.reflect.Proxy;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;

import org.junit.Test;

public class JWTClientValidatorExtensibilityTest {

    @Test
    public void validatorHooksRemainOverridable() throws Exception {
        new ExtensibleJWTClientValidator(context(), validator -> true);
    }

    private static ClientAuthenticationFlowContext context() {
        return (ClientAuthenticationFlowContext) Proxy.newProxyInstance(
                ClientAuthenticationFlowContext.class.getClassLoader(),
                new Class<?>[] { ClientAuthenticationFlowContext.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getSession":
                        case "getRealm":
                            return null;
                        case "getState":
                            return new ClientAssertionState(null, null, null, null);
                        default:
                            throw new UnsupportedOperationException(method.toString());
                    }
                });
    }

    // Compile-time API stability test: this subclass must keep compiling as
    // the supported extension point for JWT client validator customizations.
    private static class ExtensibleJWTClientValidator extends AbstractJWTClientValidator {

        private ExtensibleJWTClientValidator(ClientAuthenticationFlowContext context, SignatureValidator signatureValidator) throws Exception {
            super(context, signatureValidator, null);
        }

        @Override
        protected boolean validateClientAssertionParameters() {
            return super.validateClientAssertionParameters();
        }

        @Override
        protected boolean validateClient() {
            return super.validateClient();
        }

        @Override
        protected boolean validateSignature() {
            return super.validateSignature();
        }

        @Override
        protected boolean failure(AuthenticationFlowError error) {
            return super.failure(error);
        }

        @Override
        protected boolean failure(AuthenticationFlowError error, Response response) {
            return super.failure(error, response);
        }

        @Override
        protected String getExpectedTokenIssuer() {
            return null;
        }

        @Override
        protected List<String> getExpectedAudiences() {
            return List.of();
        }

        @Override
        protected boolean isMultipleAudienceAllowed() {
            return false;
        }

        @Override
        protected int getAllowedClockSkew() {
            return 0;
        }

        @Override
        protected int getMaximumExpirationTime() {
            return 0;
        }

        @Override
        protected boolean isReusePermitted() {
            return false;
        }

        @Override
        protected String getExpectedSignatureAlgorithm() {
            return null;
        }
    }
}
