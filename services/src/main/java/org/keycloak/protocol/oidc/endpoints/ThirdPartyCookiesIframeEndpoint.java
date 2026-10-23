/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.endpoints;

import java.util.HashMap;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.forms.login.freemarker.model.NonceBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.FreemarkerUtils;
import org.keycloak.utils.MediaType;

import static org.keycloak.protocol.oidc.endpoints.IframeUtil.returnIframe;

import org.jboss.logging.Logger;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ThirdPartyCookiesIframeEndpoint {

    private static final Logger logger = Logger.getLogger(ThirdPartyCookiesIframeEndpoint.class);

    private final KeycloakSession session;

    public ThirdPartyCookiesIframeEndpoint(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("step1.html")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response step1(@QueryParam("version") String version) {
        return returnIframe(version, session, () -> loadTemplate("3p-cookies-step1.ftl"));
    }

    @GET
    @Path("step2.html")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response step2(@QueryParam("version") String version) {
        return returnIframe(version, session, () -> loadTemplate("3p-cookies-step2.ftl"));
    }

    private String loadTemplate(String template) {
        final var map = new HashMap<String, Object>();
        map.put("nonce", new NonceBean());
        try {
            return FreemarkerUtils.loadTemplateFromClasspath(map, template, getClass());
        } catch (Exception e) {
            logger.errorf(e, "Failure when loading %s", template);
            return null;
        }
    }
}
