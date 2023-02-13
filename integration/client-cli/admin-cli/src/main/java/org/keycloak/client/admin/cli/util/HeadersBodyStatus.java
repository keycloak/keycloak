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
package org.keycloak.client.admin.cli.util;

import org.keycloak.util.JsonSerialization;

import java.io.InputStream;
import java.util.Map;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class HeadersBodyStatus extends HeadersBody {

    private final String status;

    public HeadersBodyStatus(String status, Headers headers, InputStream body) {
        super(headers, body);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    private String getStatusCodeAndReason() {
        return getStatus().substring(9);
    }

    public void checkSuccess() {
        int code = getStatusCode();
        if (code < 200 || code >= 300) {
            String content = readBodyString();
            Map<String, String> error = null;
            try {
                error = JsonSerialization.readValue(content, Map.class);
            } catch (Exception ignored) {
            }

            String message = null;
            if (error != null) {
                String description = error.get("error_description");
                String err = error.get("error");
                String msg = error.get("errorMessage");
                message = msg != null ? msg : err != null ? (description + " ["+ error.get("error") + "]") : null;
            }
            throw new HttpResponseException(getStatusCodeAndReason(), message, new RuntimeException(content));
        }
    }

    public int getStatusCode() {
        return Integer.valueOf(status.split(" ")[1]);
    }
}
