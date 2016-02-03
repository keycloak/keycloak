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

package org.keycloak.common.enums;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public enum RelativeUrlsUsed {

    /**
     * Always use relative URI and resolve them later based on browser HTTP request
     */
    ALL_REQUESTS,

    /**
     * Use relative Uris just for browser requests and resolve those based on browser HTTP requests.
     * Backend request (like refresh token request, codeToToken request etc) will use the URI based on current hostname
     */
    BROWSER_ONLY,

    /**
     * Relative Uri not used. Configuration contains absolute URI
     */
    NEVER;

    public boolean useRelative(boolean isBrowserReq) {
        switch (this) {
            case ALL_REQUESTS:
                return true;
            case NEVER:
                return false;
            case BROWSER_ONLY:
                return isBrowserReq;
            default:
                return true;
        }
    }
}
