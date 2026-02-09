/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Utility methods for manipulating JSON objects.
 */
public class JsonUtils {

    // A character in a claim component is either a literal character escaped by a backslash (\., \\, \_, \q, etc.)
    // or any character other than backslash (escaping) and dot (claim component separator)
    private static final Pattern CLAIM_COMPONENT = Pattern.compile("^((\\\\.|[^\\\\.])+?)\\.");
    private static final Pattern BACKSLASHED_CHARACTER = Pattern.compile("\\\\(.)");

    /**
     * Splits the given {@code claim} into separate paths if the value contains separators as per {@link #CLAIM_COMPONENT}.
     *
     * @param claim the claim
     * @return a list with the paths
     */
    public static List<String> splitClaimPath(String claim) {
        final LinkedList<String> claimComponents = new LinkedList<>();
        Matcher m = CLAIM_COMPONENT.matcher(claim);
        int start = 0;
        while (m.find()) {
            claimComponents.add(BACKSLASHED_CHARACTER.matcher(m.group(1)).replaceAll("$1"));
            start = m.end();
            // This is necessary to match the start of region as the start of string as determined by ^
            m.region(start, claim.length());
        }
        if (claim.length() > start) {
            claimComponents.add(BACKSLASHED_CHARACTER.matcher(claim.substring(start)).replaceAll("$1"));
        }
        return claimComponents;
    }

    /**
     * Maps the Claim with the value {@code attributeValue} into the {@code jsonObject} under the path {@code split}.
     * <br />
     * <b>Input</b>
     * <ul>
     * <li>split = ["user", "profile", "email"]</li>
     * <li>attributeValue = "alice@example.com"</li>
     * <li>isMultivalued = false</li>
     * <li>jsonObject = {"foo": "bar"}</li>
     * </ul>
     * <br />
     * <b>Output</b>
     * <pre>
     * {@code
     * {
     *   "user": {
     *     "profile": {
     *       "email": "alice@example.com"
     *     }
     *   },
     *   "foo": "bar"
     * }
     * }
     * </pre>
     *
     * @param split The claim path (as created by {@link #splitClaimPath(String)}) of the (sub-)claim to map into.
     * @param attributeValue the value to map into the claim.
     * @param jsonObject the object that contains the claims (e.g., the value that will be used as the claim container)
     * @param isMultivalued whether the claim should be treated as multivalued (i.e., multiple values for the same
     *                      claim should be built into a JSON array).
     */
    public static void mapClaim(List<String> split, Object attributeValue, Map<String, Object> jsonObject, boolean isMultivalued) {
        final int length = split.size();
        int i = 0;
        for (String component : split) {
            i++;
            if (i == length && !isMultivalued) {
                jsonObject.put(component, attributeValue);
            } else if (i == length) {
                Object values = jsonObject.get(component);
                if (values == null) {
                    jsonObject.put(component, attributeValue);
                } else {
                    Collection collectionValues = values instanceof Collection ? (Collection) values : Stream.of(values).collect(Collectors.toSet());
                    if (attributeValue instanceof Collection) {
                        ((Collection) attributeValue).stream().forEach(val -> {
                            if (!collectionValues.contains(val))
                                collectionValues.add(val);
                        });
                    } else if (!collectionValues.contains(attributeValue)) {
                        collectionValues.add(attributeValue);
                    }
                    jsonObject.put(component, collectionValues);
                }
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) jsonObject.get(component);

                if (nested == null) {
                    nested = new HashMap<>();
                    jsonObject.put(component, nested);
                }

                jsonObject = nested;
            }
        }
    }

    /**
     * Determines if the given {@code claim} contains paths.
     *
     * @param claim the claim
     * @return {@code true} if the {@code claim} contains paths. Otherwise, false.
     */
    public static boolean hasPath(String claim) {
        return CLAIM_COMPONENT.matcher(claim).find();
    }

    /**
     * <p>Returns the value corresponding to the given {@code claim}.
     *
     * @param node the JSON node
     * @param claim the claim
     * @return the value
     */
    public static Object getJsonValue(JsonNode node, String claim) {
        if (node != null) {
            List<String> fields = splitClaimPath(claim);
            if (fields.isEmpty() || claim.endsWith(".")) {
                return null;
            }

            JsonNode currentNode = node;
            for (String currentFieldName : fields) {

                // if array path, retrieve field name and index
                String currentNodeName = currentFieldName;
                int arrayIndex = -1;
                if (currentFieldName.endsWith("]")) {
                    int bi = currentFieldName.indexOf("[");
                    if (bi == -1) {
                        return null;
                    }
                    try {
                        String is = currentFieldName.substring(bi + 1, currentFieldName.length() - 1).trim();
                        arrayIndex = Integer.parseInt(is);
                        if( arrayIndex < 0) throw new ArrayIndexOutOfBoundsException();
                    } catch (Exception e) {
                        return null;
                    }
                    currentNodeName = currentFieldName.substring(0, bi).trim();
                }

                currentNode = currentNode.get(currentNodeName);
                if (arrayIndex > -1 && currentNode.isArray()) {
                    currentNode = currentNode.get(arrayIndex);
                }

                if (currentNode == null) {
                    return null;
                }

                if (currentNode.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode childNode : currentNode) {
                        if (childNode.isTextual()) {
                            values.add(childNode.textValue());
                        }
                    }
                    if (values.isEmpty()) {
                        return null;
                    }
                    return values ;
                } else if (currentNode.isNull()) {
                    return null;
                } else if (currentNode.isValueNode()) {
                    String ret = currentNode.asText();
                    if (ret != null && !ret.trim().isEmpty())
                        return ret.trim();
                    else
                        return null;
                }

            }
            return currentNode;
        }
        return null;
    }
}
