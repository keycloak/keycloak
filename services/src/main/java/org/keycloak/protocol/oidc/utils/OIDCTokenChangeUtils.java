package org.keycloak.protocol.oidc.utils;

import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Utils to change the content of tokens.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 * @version $Revision: 1 $
 */
public final class OIDCTokenChangeUtils {

    private OIDCTokenChangeUtils() {
        throw new AssertionError();
    }

    // TODO@dfr: make sure that certain claims cannot be removed
    public static Set<String> removeClaims(ObjectNode token, Collection<String> claimsToRemove) {
        final Set<String> removedClaims = new TreeSet<>();

        for (String claimToRemove : claimsToRemove) {
            boolean removed = removeClaim(token, claimToRemove);
            if (removed) {
                removedClaims.add(claimToRemove);
            }
        }

        return removedClaims;
    }

    private static boolean removeClaim(ObjectNode token, String claimToRemove) {
        List<String> claimComponents = splitClaimPath(claimToRemove);
        if (claimComponents.isEmpty()) {
            return false;
        }

        boolean removed = false;

        final int length = claimComponents.size();
        int i = 0;

        ObjectNode currentObject = token;
        for (String component : claimComponents) {
            i++;
            if (i == length) {
                JsonNode removedObj = currentObject.remove(component);
                removed = removedObj != null;
            } else {
                JsonNode nested = currentObject.get(component);

                if (!(nested instanceof ObjectNode)) {
                    return false;
                }

                currentObject = (ObjectNode) nested;
            }
        }

        return removed;
    }

    // TODO@dfr: make sure that certain claims cannot be copied
    public static void copyClaims(ObjectNode srcToken, ObjectNode targetToken, Collection<String> claimsToCopy) {
        claimsToCopy.forEach(claimToCopy -> {
            copyClaim(srcToken, targetToken, claimToCopy);
        });

    }

    private static void copyClaim(ObjectNode srcToken, ObjectNode targetToken, String claimToCopy) {
        JsonNode srcValue = getClaim(srcToken, claimToCopy);

        if (srcValue != null) {
            setClaim(targetToken, claimToCopy, srcValue);
        }
    }

    private static JsonNode getClaim(ObjectNode token, String claimToGet) {
        List<String> claimComponents = splitClaimPath(claimToGet);
        if (claimComponents.isEmpty()) {
            return null;
        }

        JsonNode found = null;

        final int length = claimComponents.size();
        int i = 0;
        ObjectNode currentObject = token;
        for (String component : claimComponents) {
            i++;
            if (i == length) {
                found = currentObject.get(component);
            } else {
                JsonNode nested = currentObject.get(component);

                if (nested == null) {
                    return null;
                } else if (!(nested instanceof ObjectNode)) {
                    // TODO@dfr: logging
                    return null;
                }

                currentObject = (ObjectNode) nested;
            }
        }

        return found;
    }

    private static void setClaim(ObjectNode token, String claimToSet, JsonNode attributeValue) {
        if (attributeValue == null) {
            return;
        }

        List<String> claimComponents = splitClaimPath(claimToSet);
        if (claimComponents.isEmpty()) {
            return;
        }

        final int length = claimComponents.size();
        int i = 0;
        ObjectNode currentObject = token;
        for (String component : claimComponents) {
            i++;
            if (i == length) {
                currentObject.set(component, attributeValue);
            } else {
                JsonNode nested = currentObject.get(component);

                if (nested == null) {
                    nested = currentObject.objectNode();
                    currentObject.set(component, nested);
                } else if (!(nested instanceof ObjectNode)) {
                    // TODO@dfr: logging
                    return;
                }

                currentObject = (ObjectNode) nested;
            }
        }
    }

    private static List<String> splitClaimPath(String claimPath) {
        return OIDCAttributeMapperHelper.splitClaimPath(claimPath);
    }
}
