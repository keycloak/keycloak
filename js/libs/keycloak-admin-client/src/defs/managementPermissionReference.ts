export interface ManagementPermissionReference {
  enabled?: boolean;
  resource?: string;
  scopePermissions?: Record<string, string>;
}
