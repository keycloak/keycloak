import CrossDomainTrustConfig from "@keycloak/keycloak-admin-client/lib/defs/crossDomainTrustConfig";

/**
 * Helper function to serialize the cross-domain trust configuration
 * @param attributes realm attributes
 * @returns the serialized assertion grant configuration
 */
export const serializeCrossDomainTrustConfig = (
  config: CrossDomainTrustConfig[],
): string => {
  if (config) {
    try {
      return JSON.stringify(config);
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
  config: string,
): CrossDomainTrustConfig[] => {
  if (config) {
    try {
      return JSON.parse(config) as CrossDomainTrustConfig[];
    } catch {
      return [];
    }
  } else {
    return [];
  }
};
