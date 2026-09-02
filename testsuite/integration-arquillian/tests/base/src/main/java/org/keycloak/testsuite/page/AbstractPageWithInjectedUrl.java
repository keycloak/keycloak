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

package org.keycloak.testsuite.page;

import java.net.URISyntaxException;
import java.net.URL;

import jakarta.ws.rs.core.UriBuilder;

/**
 *
 * @author tkyjovsk
 */
@Deprecated
public abstract class AbstractPageWithInjectedUrl extends AbstractPage {

    public abstract URL getInjectedUrl();

    protected URL createInjectedURL(String url) {
        return null;
    }

    @Override
    public UriBuilder createUriBuilder() {
        try {
            return UriBuilder.fromUri(getInjectedUrl().toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
