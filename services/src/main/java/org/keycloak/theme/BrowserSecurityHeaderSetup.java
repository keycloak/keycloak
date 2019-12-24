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

import javax.swing.text.html.Option;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrowserSecurityHeaderSetup {

    public static class Options {

        private String allowedFrameSrc;

        public static Options create() {
            return new Options();
        }

        public Options allowFrameSrc(String source) {
            allowedFrameSrc = source;
            return this;
        }

        public Options build() {
            return this;
        }
    }

    public static Response.ResponseBuilder headers(Response.ResponseBuilder builder, RealmModel realm) {
        return headers(builder, realm.getBrowserSecurityHeaders(), null);
    }


    public static Response.ResponseBuilder headers(Response.ResponseBuilder builder, RealmModel realm, Options options) {
        return headers(builder, realm.getBrowserSecurityHeaders(), options);
    }

    public static Response.ResponseBuilder headers(Response.ResponseBuilder builder) {
        return headers(builder, BrowserSecurityHeaders.defaultHeaders, null);
    }

    private static Response.ResponseBuilder headers(Response.ResponseBuilder builder, Map<String, String> headers, Options options) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String header = BrowserSecurityHeaders.headerAttributeMap.get(entry.getKey());
            String value = entry.getValue();

            if (options != null) {
                if (header.equals(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY) && value.equals(BrowserSecurityHeaders.CONTENT_SECURITY_POLICY_DEFAULT) && options.allowedFrameSrc != null) {
                    value = "frame-src " + options.allowedFrameSrc + "; frame-ancestors 'self'; object-src 'none';";
                }
            }

            if (header != null && value != null && !value.isEmpty()) {
                builder.header(header, value);
            }
        }
        return builder;
    }

}
