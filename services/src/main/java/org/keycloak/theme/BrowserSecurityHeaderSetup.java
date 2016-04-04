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

package org.keycloak.theme;

import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.RealmModel;

import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrowserSecurityHeaderSetup {

    public static Response.ResponseBuilder headers(Response.ResponseBuilder builder, RealmModel realm) {
        return headers(builder, realm.getBrowserSecurityHeaders());
    }

    public static Response.ResponseBuilder headers(Response.ResponseBuilder builder, Map<String, String> headers) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String headerName = BrowserSecurityHeaders.headerAttributeMap.get(entry.getKey());
            if (headerName != null && entry.getValue() != null && entry.getValue().length() > 0) {
                builder.header(headerName, entry.getValue());
            }
        }
        return builder;
    }

}
