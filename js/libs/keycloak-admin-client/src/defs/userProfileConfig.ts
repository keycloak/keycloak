// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPConfig.java
export default interface UserProfileConfig {
  attributes?: UserProfileAttribute[];
  groups?: UserProfileGroup[];
}

// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPAttribute.java
export interface UserProfileAttribute {
  name?: string;
  validations?: Record<string, Record<string, unknown>>;
  validators?: Record<string, unknown>;
  annotations?: Record<string, unknown>;
  required?: UserProfileAttributeRequired;
  readOnly?: boolean;
  permissions?: UserProfileAttributePermissions;
  selector?: UserProfileAttributeSelector;
  displayName?: string;
  group?: string;
}

// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPAttributeRequired.java
export interface UserProfileAttributeRequired {
  roles?: string[];
  scopes?: string[];
}

// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPAttributePermissions.java
export interface UserProfileAttributePermissions {
  view?: string[];
  edit?: string[];
}

// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPAttributeSelector.java
export interface UserProfileAttributeSelector {
  scopes?: string[];
}

// See: https://github.com/keycloak/keycloak/blob/main/services/src/main/java/org/keycloak/userprofile/config/UPGroup.java
export interface UserProfileGroup {
  name?: string;
  displayHeader?: string;
  displayDescription?: string;
  annotations?: Record<string, unknown>;
}
