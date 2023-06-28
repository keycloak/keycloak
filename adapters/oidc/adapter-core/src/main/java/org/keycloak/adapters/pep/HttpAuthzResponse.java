/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.pep;

import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.authorization.spi.HttpResponse;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class HttpAuthzResponse implements HttpResponse {

    private OIDCHttpFacade oidcFacade;

    public HttpAuthzResponse(OIDCHttpFacade oidcFacade) {
        this.oidcFacade = oidcFacade;
    }

    @Override
    public void sendError(int statusCode) {
        oidcFacade.getResponse().setStatus(statusCode);
    }

    @Override
    public void sendError(int code, String reason) {
        oidcFacade.getResponse().sendError(code, reason);
    }

    @Override
    public void setHeader(String name, String value) {
        oidcFacade.getResponse().setHeader(name, value);
    }

}
