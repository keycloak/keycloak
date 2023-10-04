// See: https://github.com/keycloak/keycloak/blob/main/core/src/main/java/org/keycloak/representations/idm/UserProfileMetadata.java
export interface UserProfileMetadata {
  attributes?: UserProfileAttributeMetadata[];
  groups?: UserProfileAttributeGroupMetadata[];
}

// See: https://github.com/keycloak/keycloak/blob/main/core/src/main/java/org/keycloak/representations/idm/UserProfileAttributeMetadata.java
export interface UserProfileAttributeMetadata {
  name?: string;
  displayName?: string;
  required?: boolean;
  readOnly?: boolean;
  group?: string;
  annotations?: Record<string, unknown>;
  validators?: Record<string, Record<string, unknown>>;
}

// See: https://github.com/keycloak/keycloak/blob/main/core/src/main/java/org/keycloak/representations/idm/UserProfileAttributeGroupMetadata.java
export interface UserProfileAttributeGroupMetadata {
  name?: string;
  displayHeader?: string;
  displayDescription?: string;
  annotations?: Record<string, unknown>;
}
