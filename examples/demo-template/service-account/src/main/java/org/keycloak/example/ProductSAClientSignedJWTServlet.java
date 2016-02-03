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

package org.keycloak.example;

/**
 * Client authentication based on JWT signed by client private key .
 * See Keycloak documentation and <a href="https://tools.ietf.org/html/rfc7519">specs</a> for more details.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ProductSAClientSignedJWTServlet extends ProductServiceAccountServlet {

    @Override
    protected String getAdapterConfigLocation() {
        return "/WEB-INF/keycloak-client-signed-jwt.json";
    }

    @Override
    protected String getClientAuthenticationMethod() {
        return "jwt";
    }
}
