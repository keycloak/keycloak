package org.keycloak.protocol.oidc.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
        JsonNode claims = JsonSerialization.readValue(claimsParam, JsonNode.class);
        JsonNode idToken = claims.get("id_token");
        if (idToken != null) {
          JsonNode acrClaim = idToken.get(IDToken.ACR);
          if (acrClaim != null) {
            JsonNode essential = acrClaim.get("essential");
            if (notEssential || (essential != null && essential.isBoolean() && essential.booleanValue())) {
              JsonNode values = acrClaim.get("values");
              if (values != null && values.isArray()) {
                for (JsonNode value : values) {
                  if (value.isTextual()) {
                    acrValues.add(value.textValue());
                  }
                }
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
      for (String requestedAcr : acrValues) {
        Integer mappedLoa = acrLoaMap.get(requestedAcr);
        if (mappedLoa != null && mappedLoa > maxLoa && loa >= mappedLoa) {
          acr = requestedAcr;
          maxLoa = mappedLoa;
        }
      }
    }
    return acr;
  }
}
