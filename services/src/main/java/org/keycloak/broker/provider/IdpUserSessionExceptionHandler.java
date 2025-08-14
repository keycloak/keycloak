/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.broker.provider;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.saml.BaseSAML2BindingBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class IdpUserSessionExceptionHandler implements UserSessionProvider.UserSessionExceptionHandler {

    private final KeycloakContext ctx;

    protected static final Logger logger = Logger.getLogger(IdpUserSessionExceptionHandler.class);

    public IdpUserSessionExceptionHandler(KeycloakSession session) {
        this.ctx = session.getContext();
    }

    @Override
    public Response handleExceptionAtTransactionCommit(Throwable t) {
        if (isModelDuplicateException(t)) {

            String httpMethod = this.ctx.getHttpRequest().getHttpMethod();

            switch (httpMethod) {
                case "GET":
                    URI requestUrl = this.ctx.getUri().getRequestUri();
                    logger.debugf("Exception happened during transaction commit. Retrying request to the URL: %s", requestUrl);
                    Response.ResponseBuilder location = Response.status(302).location(requestUrl);
                    return location.build();
                case "POST":
                    URI postUrl = this.ctx.getUri().getRequestUri();
                    logger.debugf("Exception happened during transaction commit. Retrying POST request to the URL: %s", postUrl);
                    Map<String, String> formParams = this.ctx.getHttpRequest().getDecodedFormParameters()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get(0)));
                    String response = BaseSAML2BindingBuilder.buildHtmlForm(postUrl.toString(), formParams);
                    return Response.status(Response.Status.OK)
                            .type(MediaType.TEXT_HTML_TYPE)
                            .entity(response).build();
                default:
                    logger.warn("Unknown HTTP method, skipping");
                    return null;
            }
        }
        return null;
    }

    private boolean isModelDuplicateException(Throwable e) {
        if (e instanceof ModelDuplicateException) return true;
        if (e.getCause() != null && e.getCause() instanceof ModelDuplicateException) return true;
        if (e.getSuppressed() != null) {
            for (Throwable t : e.getSuppressed()) {
                if (t instanceof ModelDuplicateException) return true;
            }
        }
        return false;
    }
}
