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

package org.keycloak.adapters.authorization.integration.elytron;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.keycloak.adapters.authorization.spi.HttpResponse;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ServletHttpResponse implements HttpResponse {

    private HttpServletResponse response;

    public ServletHttpResponse(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void sendError(int status) {
        try {
            response.sendError(status);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendError(int status, String reason) {
        try {
            response.sendError(status, reason);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }
}
