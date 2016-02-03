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

package org.keycloak.broker.oidc.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.broker.provider.util.SimpleHttp;

import java.io.IOException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JsonSimpleHttp extends SimpleHttp {
    public JsonSimpleHttp(String url, String method) {
        super(url, method);
    }

    public static JsonSimpleHttp doGet(String url) {
        return new JsonSimpleHttp(url, "GET");
    }

    public static JsonSimpleHttp doPost(String url) {
        return new JsonSimpleHttp(url, "POST");
    }

    private static ObjectMapper mapper = new ObjectMapper();

    public static JsonNode asJson(SimpleHttp request) throws IOException {
        return mapper.readTree(request.asString());
    }

}
