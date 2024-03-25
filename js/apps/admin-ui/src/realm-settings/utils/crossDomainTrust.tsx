import CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";

/**
 * Helper function to serialize the cross-domain trust configuration
 * @param attributes realm attributes
 * @returns the serialized assertion grant configuration
 */
export const serializeCrossDomainTrustConfig = (
  attributes: Record<string, any>,
): string => {
  if (attributes["crossDomainTrust"]) {
    try {
      return JSON.stringify(attributes["crossDomainTrust"]);
    } catch {
      return "[]";
    }
  } else {
    return "[]";
  }
};

/**
 * Helper function to deserialize the cross-domain trust configuration
 * @param attributes realm attributes
 * @returns the list of cross-domain trust configurations
 */
export const deserializeCrossDomainTrustConfig = (
  attributes: Record<string, any>,
): CrossDomainTrustConfig[] => {
  if (attributes["crossDomainTrust"]) {
    try {
      return JSON.parse(
        attributes["crossDomainTrust"],
      ) as CrossDomainTrustConfig[];
    } catch {
      return [];
    }
  } else {
    return [];
  }
};
