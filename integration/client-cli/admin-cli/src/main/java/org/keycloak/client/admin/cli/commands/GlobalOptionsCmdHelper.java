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
package org.keycloak.client.admin.cli.commands;

import java.io.IOException;

import org.keycloak.client.cli.util.FilterUtil;
import org.keycloak.client.cli.util.ReturnFields;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.keycloak.client.cli.util.HttpUtil.normalize;

public interface GlobalOptionsCmdHelper {

    default String composeAdminRoot(String server) {
        return normalize(server) + "admin";
    }

    default String extractTypeNameFromUri(String resourceUrl) {
        String type = extractLastComponentOfUri(resourceUrl);
        if (type.endsWith("s")) {
            type = type.substring(0, type.length()-1);
        }
        return type;
    }

    default String extractLastComponentOfUri(String resourceUrl) {
        int endPos = resourceUrl.endsWith("/") ? resourceUrl.length()-2 : resourceUrl.length()-1;
        int pos = resourceUrl.lastIndexOf("/", endPos);
        pos = pos == -1 ? 0 : pos;
        return resourceUrl.substring(pos+1, endPos+1);
    }

    default JsonNode applyFieldFilter(ObjectMapper mapper, JsonNode rootNode, ReturnFields returnFields) {
        // construct new JsonNode that satisfies filtering specified by returnFields
        try {
            return FilterUtil.copyFilteredObject(rootNode, returnFields);
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply fields filter", e);
        }
    }

}
