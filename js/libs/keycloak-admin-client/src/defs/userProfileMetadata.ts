export interface UserProfileConfig {
  attributes?: UserProfileAttribute[];
  groups?: UserProfileGroup[];
  unmanagedAttributePolicy?: UnmanagedAttributePolicy;
}
export interface UserProfileAttribute {
  name?: string;
  validations?: Record<string, unknown>;
  validators?: Record<string, unknown>;
  annotations?: Record<string, unknown>;
  required?: UserProfileAttributeRequired;
  readOnly?: boolean;
  permissions?: UserProfileAttributePermissions;
  selector?: UserProfileAttributeSelector;
  displayName?: string;
  group?: string;
  multivalued?: boolean;
  defaultValue?: string;
}
export interface UserProfileAttributeRequired {
  roles?: string[];
  scopes?: string[];
}
export interface UserProfileAttributePermissions {
  view?: string[];
  edit?: string[];
}
export interface UserProfileAttributeSelector {
  scopes?: string[];
}
export interface UserProfileGroup {
  name?: string;
  displayHeader?: string;
  displayDescription?: string;
  annotations?: Record<string, unknown>;
}

export interface UserProfileAttributeMetadata {
  name?: string;
  displayName?: string;
  required?: boolean;
  readOnly?: boolean;
  group?: string;
  annotations?: Record<string, unknown>;
  validators?: Record<string, Record<string, unknown>>;
  multivalued?: boolean;
  defaultValue?: string;
}

export interface UserProfileAttributeGroupMetadata {
  name?: string;
  displayHeader?: string;
  displayDescription?: string;
  annotations?: Record<string, unknown>;
}

export interface UserProfileMetadata {
  attributes?: UserProfileAttributeMetadata[];
  groups?: UserProfileAttributeGroupMetadata[];
}

export enum UnmanagedAttributePolicy {
  Disabled = "DISABLED",
  Enabled = "ENABLED",
  AdminView = "ADMIN_VIEW",
  AdminEdit = "ADMIN_EDIT",
}
