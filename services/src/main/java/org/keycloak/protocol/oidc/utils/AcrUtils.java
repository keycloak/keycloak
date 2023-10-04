/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oidc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

public class AcrUtils {

    private static final Logger LOGGER = Logger.getLogger(AcrUtils.class);

    public static List<String> getRequiredAcrValues(String claimsParam) {
        return getAcrValues(claimsParam, null, true);
    }

    public static List<String> getAcrValues(String claimsParam, String acrValuesParam, ClientModel client) {
        List<String> fromParams = getAcrValues(claimsParam, acrValuesParam, false);
        if (!fromParams.isEmpty()) {
            return fromParams;
        }

        // Fallback to default ACR values of client (if configured)
        return getDefaultAcrValues(client);
    }

    private static List<String> getAcrValues(String claimsParam, String acrValuesParam, boolean essential) {
        List<String> acrValues = new ArrayList<>();
        if (acrValuesParam != null && !essential) {
            acrValues.addAll(Arrays.asList(acrValuesParam.split(" ")));
        }
        if (claimsParam != null) {
            try {
                ClaimsRepresentation claims = JsonSerialization.readValue(claimsParam, ClaimsRepresentation.class);
                if (claims == null) {
                    LOGGER.warnf("Invalid claims parameter. Claims parameter should be JSON");
                } else {
                    ClaimsRepresentation.ClaimValue<String> acrClaim = claims.getClaimValue(IDToken.ACR, ClaimsRepresentation.ClaimContext.ID_TOKEN, String.class);
                    if (acrClaim != null) {
                        if (!essential || acrClaim.isEssential()) {
                            if (acrClaim.getValues() != null) {
                                acrValues.addAll(acrClaim.getValues());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Invalid claims parameter", e);
            }
        }
        return acrValues;
    }

    /**
     * @param client
     * @return map corresponding to "acr-to-loa" client attribute. It will fallback to realm in case "acr-to-loa" mapping not configured on client
     */
    public static Map<String, Integer> getAcrLoaMap(ClientModel client) {
        Map<String, Integer> result = getAcrLoaMapForClientOnly(client);
        if (result.isEmpty()) {
            // Fallback to realm
            return getAcrLoaMap(client.getRealm());
        } else {
            return result;
        }
    }


    private static Map<String, Integer> getAcrLoaMapForClientOnly(ClientModel client) {
        String map = client.getAttribute(Constants.ACR_LOA_MAP);
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonSerialization.readValue(map, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            LOGGER.warnf("Invalid client configuration (ACR-LOA map) for client '%s'", client.getClientId());
            return Collections.emptyMap();
        }
    }

    /**
     * @param realm
     * @return map corresponding to "acr-to-loa" realm attribute.
     */
    public static Map<String, Integer> getAcrLoaMap(RealmModel realm) {
        String map = realm.getAttribute(Constants.ACR_LOA_MAP);
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonSerialization.readValue(map, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            LOGGER.warn("Invalid realm configuration (ACR-LOA map)");
            return Collections.emptyMap();
        }
    }

    public static String mapLoaToAcr(int loa, Map<String, Integer> acrLoaMap, Collection<String> acrValues) {
        String acr = null;
        if (!acrLoaMap.isEmpty() && !acrValues.isEmpty()) {
            int maxLoa = -1;
            for (String acrValue : acrValues) {
                Integer mappedLoa = acrLoaMap.get(acrValue);
                // if there is no mapping for the acrValue, it may be an integer itself
                if (mappedLoa == null) {
                    try {
                        mappedLoa = Integer.parseInt(acrValue);
                    } catch (NumberFormatException e) {
                        // the acrValue cannot be mapped
                        LOGGER.warnf("Acr value '%s' cannot be mapped to int", acrValue);
                    }
                }
                if (mappedLoa != null && mappedLoa > maxLoa && loa >= mappedLoa) {
                    acr = acrValue;
                    maxLoa = mappedLoa;
                }
            }
        }
        return acr;
    }


    public static List<String> getDefaultAcrValues(ClientModel client) {
        return OIDCAdvancedConfigWrapper.fromClientModel(client).getAttributeMultivalued(Constants.DEFAULT_ACR_VALUES);
    }

    /**
     * Helper function to extract ACR to auth flow mapping for the realm
     * @param realm The realm the request is taking place in
     * @return acr to auth flow mapping
     */

    public static Map<String, String> getAcrFlowMap(RealmModel realm) {
        String map = realm.getAttribute(Constants.ACR_FLOW_MAP);
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonSerialization.readValue(map, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            LOGGER.warn("Invalid realm configuration (ACR-FLOW map)");
            return Collections.emptyMap();
        }
    }

    /**
     * Helper function to extract ACR to auth flow mapping, checking the client and then defaulting to the realm config
     * @param client The client that is placing the request
     * @return map of the acr to auth flow mapping
     */
    public static Map<String, String> getAcrFlowMap(ClientModel client) {
        Map<String, String> result = getAcrFlowMapForClient(client);
        if (result.isEmpty()) {
            // Fallback to realm
            return getAcrFlowMap(client.getRealm());
        } else {
            return result;
        }
    }

    /**
     * Helper function to extract and parse the ACR to auth flow mapping from the client attributes
     * @param client The client that is requesting authentication
     * @return map of the acr to auth flow mapping configured on the client
     */
    public static Map<String, String> getAcrFlowMapForClient(ClientModel client){
        String map = client.getAttribute(Constants.ACR_FLOW_MAP);
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return JsonSerialization.readValue(map, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            LOGGER.warnf("Invalid client configuration (ACR-FLOW map) for client '%s'", client.getClientId());
        }

        return Collections.emptyMap();
    }

    /**
     * Helper function to fetch the first authentication flow ID based on a set of requested acr values.
     *
     * @param acrValues - the set of acr values requested by the client
     * @param acrFlowMap - the acr authentication flow bindings configured for a given client
     * @return The authentication flow ID for the first acr value found in the acrFlowMap
     */
    public static String mapAcrToFlow(Collection<String> acrValues, Map<String, String> acrFlowMap){
        for (String context: acrValues){
            if (acrFlowMap.containsKey(context)){
                return acrFlowMap.get(context);
            }
        }

        return null;
    }

    /**
     * Helper function to fetch the acr value associated with the completed authentication flow.
     *
     * @param flowId - The authentication flow to search for in the acrFlowMap
     * @param acrValues - The set of acr values requested by the client
     * @param acrFlowMap - The acr authentication flow bindings configured for a given client
     * @return The acr value associated with the given flowId
     */
    public static String mapFlowToAcr(String flowId, Collection<String> acrValues, Map<String, String> acrFlowMap){
        for (Map.Entry<String, String> entry: acrFlowMap.entrySet()){
            if (entry.getValue().equals(flowId) && acrValues.contains(entry.getKey())){
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Helper function to fetch the completed flow id from the client session. This value is transferred from the
     * auth session to the client session when the auth session is attached to the client session
     * @param clientSession The authentication session for the client placing the request
     * @return The ID of the authentication flow that was completed in the supplied client session
     */
    public static String getCompletedFlowId(AuthenticatedClientSessionModel clientSession) {
        String completedFlowNote = clientSession.getUserSession().getNote(Constants.COMPLETED_FLOW_ID);
        return completedFlowNote == null ? "" : completedFlowNote;
    }
}
