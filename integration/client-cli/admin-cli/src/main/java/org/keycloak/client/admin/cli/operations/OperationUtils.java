/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.client.admin.cli.operations;

import java.util.List;
import java.util.function.Supplier;

import org.keycloak.client.cli.util.HttpUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.keycloak.common.util.ObjectUtil.capitalize;

public class OperationUtils {

    private static final String[] DEFAULT_QUERY_PARAMS = { "first", "0", "max", "2" };

    public static String getIdForType(String rootUrl, String realm, String auth, String resourceEndpoint, String attrName, String attrValue, String inputAttrName) {

        return getAttrForType(rootUrl, realm, auth, resourceEndpoint, attrName, attrValue, inputAttrName, "id", null);
    }

    public static String getIdForType(String rootUrl, String realm, String auth, String resourceEndpoint, String attrName, String attrValue, String inputAttrName, Supplier<String[]> endpointParams) {
        return getAttrForType(rootUrl, realm, auth, resourceEndpoint, attrName, attrValue, inputAttrName, "id", endpointParams);
    }

    public static String getAttrForType(String rootUrl, String realm, String auth, String resourceEndpoint, String attrName, String attrValue, String inputAttrName, String returnAttrName) {
        return getAttrForType(rootUrl, realm, auth, resourceEndpoint, attrName, attrValue, inputAttrName, returnAttrName, null);
    }

    public static String getAttrForType(String rootUrl, String realm, String auth, String resourceEndpoint, String attrName, String attrValue, String inputAttrName, String returnAttrName, Supplier<String[]> endpointParams) {
        String resourceUrl = HttpUtil.composeResourceUrl(rootUrl, realm, resourceEndpoint);
        String[] defaultParams;

        if (endpointParams == null) {
            defaultParams = DEFAULT_QUERY_PARAMS;
        } else {
            defaultParams = endpointParams.get();
        }

        resourceUrl = HttpUtil.addQueryParamsToUri(resourceUrl, attrName, attrValue);
        resourceUrl = HttpUtil.addQueryParamsToUri(resourceUrl, defaultParams);

        List<ObjectNode> results = HttpUtil.doGetJSON(RoleOperations.LIST_OF_NODES.class, resourceUrl, auth);

        ObjectNode match;
        try {
            match = new LocalSearch(results).exactMatchOne(attrValue, inputAttrName);
        } catch (Exception e) {
            throw new RuntimeException("Multiple " + resourceEndpoint + " found for " + inputAttrName + ": " + attrValue, e);
        }

        String typeName = HttpUtil.singularize(resourceEndpoint);
        if (match == null) {
            if (results.size() > 1) {
                throw new RuntimeException("Some matches, but not an exact match, found for " + capitalize(typeName) + " with " + inputAttrName + ": " + attrValue + ". Try using a more unique search, such as an id.");
            }
            throw new RuntimeException(capitalize(typeName) + " not found for " + inputAttrName + ": " + attrValue);
        }

        JsonNode attr = match.get(returnAttrName);
        if (attr == null) {
            throw new RuntimeException("Returned " + typeName + " info has no '" + returnAttrName + "' attribute");
        }
        return attr.asText();
    }

}
