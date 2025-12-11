/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.keycloak.protocol.oid4vc.model.ClaimsDescription;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * Utility class for handling claims path pointers.
 * A claims path pointer is a pointer into the Verifiable Credential, identifying one or more claims.
 *
 * @author <a href="mailto:Forkim.Akwichek@adorsys.com">Forkim Akwichek</a>
 */
public class ClaimsPathPointer {

    private static final Logger logger = Logger.getLogger(ClaimsPathPointer.class);

    /**
     * Validates a claims path pointer.
     *
     * @param path the claims path pointer to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPath(List<Object> path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        for (Object component : path) {
            if (component == null) {
                // null is valid for array selection
                continue;
            }

            if (component instanceof String) {
                // String is valid for object key selection, but should not be blank
                if (StringUtil.isBlank((String) component)) {
                    return false;
                }
                continue;
            }

            if (component instanceof Integer) {
                Integer index = (Integer) component;
                if (index < 0) {
                    // Negative integers are not allowed
                    return false;
                }
                // Non-negative integers are valid for array index selection
                continue;
            }

            // Any other type is invalid
            return false;
        }

        return true;
    }

    /**
     * Validates a list of claims descriptions for conflicts and contradictions.
     *
     * @param claims the list of claims descriptions to validate
     * @return true if valid, false if conflicts are found
     */
    public static boolean validateClaimsDescriptions(List<ClaimsDescription> claims) {
        if (claims == null || claims.isEmpty()) {
            return true;
        }

        // Check for repeated or contradictory claim descriptions
        for (int i = 0; i < claims.size(); i++) {
            for (int j = i + 1; j < claims.size(); j++) {
                ClaimsDescription claim1 = claims.get(i);
                ClaimsDescription claim2 = claims.get(j);

                if (isConflicting(claim1, claim2)) {
                    logger.warnf("Conflicting claims descriptions found: %s and %s", claim1.getPath(), claim2.getPath());
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if two claims descriptions are conflicting.
     *
     * @param claim1 first claims description
     * @param claim2 second claims description
     * @return true if conflicting, false otherwise
     */
    private static boolean isConflicting(ClaimsDescription claim1, ClaimsDescription claim2) {
        List<Object> path1 = claim1.getPath();
        List<Object> path2 = claim2.getPath();

        if (path1 == null || path2 == null) {
            return false;
        }

        // Check if paths are identical (same claim addressed)
        if (path1.equals(path2)) {
            return true;
        }

        // Check for array vs object conflicts
        return hasArrayObjectConflict(path1, path2);
    }

    /**
     * Checks if there's a conflict between array and object addressing for the same claim.
     *
     * @param path1 first path
     * @param path2 second path
     * @return true if there's an array/object conflict, false otherwise
     */
    private static boolean hasArrayObjectConflict(List<Object> path1, List<Object> path2) {
        int minLength = Math.min(path1.size(), path2.size());

        for (int i = 0; i < minLength; i++) {
            Object comp1 = path1.get(i);
            Object comp2 = path2.get(i);

            // If components are different types and one is null (array selection) and the other is string (object selection)
            if (comp1 == null && comp2 instanceof String) {
                return true;
            }
            if (comp2 == null && comp1 instanceof String) {
                return true;
            }

            // If components are different types and one is integer (specific array index) and the other is null (all array elements)
            if (comp1 == null && comp2 instanceof Integer) {
                return true;
            }
            if (comp2 == null && comp1 instanceof Integer) {
                return true;
            }

            // If components are equal, return true (as suggested by reviewer)
            if (Objects.equals(comp1, comp2)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Filters a map of claims based on authorization details claims descriptions.
     * Only claims that match the requested paths will be included in the result.
     *
     * @param allClaims       the complete map of claims to filter
     * @param requestedClaims the list of claims descriptions from authorization details
     * @return filtered map containing only the requested claims
     * @throws IllegalArgumentException if mandatory claims are missing
     */
    public static Map<String, Object> filterClaimsByAuthorizationDetails(
            Map<String, Object> allClaims,
            List<ClaimsDescription> requestedClaims) {

        if (requestedClaims == null || requestedClaims.isEmpty()) {
            return allClaims; // No filtering requested, return all claims
        }

        Map<String, Object> filteredClaims = new HashMap<>();

        for (ClaimsDescription claim : requestedClaims) {
            List<Object> path = claim.getPath();
            if (path == null || path.isEmpty()) {
                continue; // Skip invalid paths
            }

            // Validate the claims path pointer format according to OID4VCI specification
            if (!isValidPath(path)) {
                logger.warnf("Invalid claims path pointer: %s. Path must contain only strings, non-negative integers, and null values.", path);
                continue; // Skip invalid paths
            }

            try {
                // Get claim values
                List<Object> claimValues = processClaimsPathPointer(allClaims, path);

                if (!claimValues.isEmpty()) {
                    // Add all selected claim values to filtered results
                    if (claimValues.size() == 1) {
                        // Single value, use existing method
                        addClaimByPath(filteredClaims, path, claimValues.get(0));
                    } else {
                        // Multiple values from array selection, use helper method
                        addMultipleClaimsByPath(filteredClaims, path, claimValues);
                    }
                } else if (Boolean.TRUE.equals(claim.getMandatory())) {
                    // Mandatory claim is missing - this should fail
                    throw new IllegalArgumentException("Mandatory claim not found: " + path);
                }
                // Optional claims that don't exist are simply not included
            } catch (IllegalArgumentException e) {
                if (Boolean.TRUE.equals(claim.getMandatory())) {
                    // Log error for mandatory claims before re-throwing
                    logger.errorf("Failed to process mandatory claim path %s: %s", path, e.getMessage());
                    // Re-throw for mandatory claims
                    throw e;
                }
                // For optional claims, log warning and continue
                logger.warnf("Failed to process optional claim path %s: %s", path, e.getMessage());
            }
        }

        return filteredClaims;
    }


    /**
     * Processes a claims path pointer according to OID4VCI specification.
     *
     * @param claims the claims map to search in
     * @param path   the claims path pointer
     * @return the set of selected JSON elements, or empty list if none found
     * @throws IllegalArgumentException if processing fails according to spec rules
     */
    public static List<Object> processClaimsPathPointer(Map<String, Object> claims, List<Object> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Claims path pointer must be a non-empty array");
        }
        if (claims == null) {
            throw new IllegalArgumentException("Claims map cannot be null");
        }

        // Start with root element
        List<Object> currentSelection = new ArrayList<>();
        currentSelection.add(claims);

        // Process each path component from left to right
        for (Object component : path) {
            if (currentSelection.isEmpty()) {
                throw new IllegalArgumentException("No elements currently selected, cannot process further");
            }

            List<Object> nextSelection = new ArrayList<>();

            for (Object current : currentSelection) {
                if (component instanceof String) {
                    // String component: select element by key
                    if (current instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) current;
                        Object value = map.get(component);
                        if (value != null) {
                            nextSelection.add(value);
                        }
                    }
                } else if (component instanceof Integer) {
                    // Integer component: select element by index
                    int index = (Integer) component;
                    if (index < 0) {
                        throw new IllegalArgumentException("Negative integer values are not allowed in claims path pointer");
                    }
                    if (current instanceof List) {
                        List<?> list = (List<?>) current;
                        if (index < list.size()) {
                            nextSelection.add(list.get(index));
                        }
                    }
                } else if (component == null) {
                    // Null component: select all elements of currently selected array(s)
                    if (current instanceof List) {
                        List<?> list = (List<?>) current;
                        nextSelection.addAll(list);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid path component type: " + component.getClass().getSimpleName() +
                            ". Only String, Integer, and null are allowed.");
                }
            }

            currentSelection = nextSelection;
        }

        if (currentSelection.isEmpty()) {
            throw new IllegalArgumentException("No elements selected after processing claims path pointer");
        }

        return currentSelection;
    }

    /**
     * Adds multiple claim values to a claims map when array selection is involved.
     * This method properly handles paths with null components that select multiple array elements.
     *
     * @param claims the claims map to add to
     * @param path   the claims path pointer
     * @param values the list of values to add
     */
    private static void addMultipleClaimsByPath(Map<String, Object> claims, List<Object> path, List<Object> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        // For simple paths, add the first value
        if (path.size() == 1 && path.get(0) instanceof String) {
            claims.put((String) path.get(0), values.get(0));
            return;
        }

        // For complex paths with array selection, we need to handle the structure properly
        // This creates the appropriate nested structure to hold the selected values
        if (values.size() == 1) {
            // Single value, use existing method
            addClaimByPath(claims, path, values.get(0));
        } else {
            // Multiple values - this indicates array selection
            // We need to create an array structure to hold all values
            createArrayStructureForMultipleValues(claims, path, values);
        }
    }

    /**
     * Creates an array structure to hold multiple values from array selection.
     * This handles the case where a path with null components selects multiple array elements.
     *
     * @param claims the claims map to add to
     * @param path   the claims path pointer
     * @param values the list of values to add
     */
    private static void createArrayStructureForMultipleValues(Map<String, Object> claims, List<Object> path, List<Object> values) {
        buildNestedStructure(claims, path, new ArrayList<Object>(values), true);
    }

    /**
     * Adds a claim value to a claims map using a claims path pointer.
     *
     * @param claims the claims map to add to
     * @param path   the claims path pointer
     * @return the claim value, or null if not found
     */
    private static void addClaimByPath(Map<String, Object> claims, List<Object> path, Object value) {
        if (path == null || path.isEmpty() || claims == null) {
            return;
        }

        if (path.size() == 1 && path.get(0) instanceof String) {
            // Simple case: direct key assignment
            claims.put((String) path.get(0), value);
            return;
        }

        // Complex case: nested path - build the structure
        buildNestedClaimStructure(claims, path, value);
    }

    /**
     * Builds nested claim structure for complex paths.
     *
     * @param claims the claims map to build in
     * @param path   the claims path pointer
     * @param value  the value to add
     */
    private static void buildNestedClaimStructure(Map<String, Object> claims, List<Object> path, Object value) {
        buildNestedStructure(claims, path, value, false);
    }

    /**
     * Generic method to build nested structure for both single values and multiple values.
     *
     * @param claims           the claims map to build in
     * @param path             the claims path pointer
     * @param value            the value to add (single value or list of values)
     * @param isArraySelection true if this is for array selection (multiple values), false for single value
     */
    private static void buildNestedStructure(Map<String, Object> claims, List<Object> path, Object value, boolean isArraySelection) {
        if (path.size() < 2) {
            return;
        }

        Object current = claims;
        String rootKey = (String) path.get(0);

        // Ensure root key exists
        if (!(current instanceof Map)) {
            return;
        }

        Map<String, Object> rootMap = (Map<String, Object>) current;
        if (!rootMap.containsKey(rootKey)) {
            // Use ArrayList for array selection, HashMap for single values
            rootMap.put(rootKey, isArraySelection ? new ArrayList<Object>() : new HashMap<String, Object>());
        }

        current = rootMap.get(rootKey);

        // Navigate through the path, building structure as needed
        for (int i = 1; i < path.size() - 1; i++) {
            Object component = path.get(i);

            if (component instanceof String) {
                if (!(current instanceof Map)) {
                    return; // Can't navigate further
                }
                Map<String, Object> map = (Map<String, Object>) current;
                if (!map.containsKey(component)) {
                    map.put((String) component, new HashMap<String, Object>());
                }
                current = map.get(component);
            } else if (component instanceof Integer) {
                if (!(current instanceof List)) {
                    return; // Can't navigate further
                }
                List<Object> list = (List<Object>) current;
                int index = (Integer) component;
                while (list.size() <= index) {
                    // Use HashMap for single values, null for array selection
                    list.add(isArraySelection ? null : new HashMap<String, Object>());
                }
                current = list.get(index);
            }
        }

        // Set the final value
        Object finalComponent = path.get(path.size() - 1);
        if (finalComponent instanceof String) {
            if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                map.put((String) finalComponent, value);
            }
        } else if (finalComponent instanceof Integer) {
            if (current instanceof List) {
                List<Object> list = (List<Object>) current;
                int index = (Integer) finalComponent;
                while (list.size() <= index) {
                    list.add(null);
                }
                list.set(index, value);
            }
        }
    }
}
