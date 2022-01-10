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
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

public class AcrUtils {

    private static final Logger LOGGER = Logger.getLogger(AcrUtils.class);

    public static List<String> getRequiredAcrValues(String claimsParam) {
        return getAcrValues(claimsParam, null, false);
    }

    public static List<String> getAcrValues(String claimsParam, String acrValuesParam) {
        return getAcrValues(claimsParam, acrValuesParam, true);
    }

    private static List<String> getAcrValues(String claimsParam, String acrValuesParam, boolean notEssential) {
        List<String> acrValues = new ArrayList<>();
        if (acrValuesParam != null && notEssential) {
            acrValues.addAll(Arrays.asList(acrValuesParam.split(" ")));
        }
        if (claimsParam != null) {
            try {
                ClaimsRepresentation claims = JsonSerialization.readValue(claimsParam, ClaimsRepresentation.class);
                ClaimsRepresentation.ClaimValue<String> acrClaim = claims.getClaimValue(IDToken.ACR, ClaimsRepresentation.ClaimContext.ID_TOKEN, String.class);
                if (acrClaim != null) {
                    if (notEssential || acrClaim.isEssential()) {
                        if (acrClaim.getValues() != null) {
                            acrValues.addAll(acrClaim.getValues());
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Invalid claims parameter", e);
            }
        }
        return acrValues;
    }

    public static Map<String, Integer> getAcrLoaMap(ClientModel client) {
        String map = client.getAttribute(Constants.ACR_LOA_MAP);
        if (map == null || map.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonSerialization.readValue(map, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            LOGGER.warn("Invalid client configuration (ACR-LOA map)");
            return Collections.emptyMap();
        }
    }

    public static String mapLoaToAcr(int loa, Map<String, Integer> acrLoaMap, Collection<String> acrValues) {
        String acr = null;
        if (!acrLoaMap.isEmpty() && !acrValues.isEmpty()) {
            int maxLoa = 0;
            for (String acrValue : acrValues) {
                Integer mappedLoa = acrLoaMap.get(acrValue);
                // if there is no mapping for the acrValue, it may be an integer itself
                if (mappedLoa == null) {
                    try {
                        mappedLoa = Integer.parseInt(acrValue);
                    } catch (NumberFormatException e) {
                        // the acrValue cannot be mapped
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
}
