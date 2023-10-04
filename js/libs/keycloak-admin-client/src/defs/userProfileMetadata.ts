// See: https://github.com/keycloak/keycloak/blob/main/core/src/main/java/org/keycloak/representations/idm/UserProfileMetadata.java
export default interface UserProfileMetadata {
  attributes?: UserProfileAttributeMetadata[];
}

// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPAttribute.java
export interface UserProfileAttributeMetadata {
  name?: string;
  displayName?: string;
  required?: boolean;
  readOnly?: boolean;
  annotations?: Record<string, unknown>;
  validators?: Record<string, Record<string, unknown>>;
}
