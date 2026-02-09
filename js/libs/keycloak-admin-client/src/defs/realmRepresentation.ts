import type ClientRepresentation from "./clientRepresentation.js";
import type ComponentExportRepresentation from "./componentExportRepresentation.js";
import type UserRepresentation from "./userRepresentation.js";
import type GroupRepresentation from "./groupRepresentation.js";
import type IdentityProviderRepresentation from "./identityProviderRepresentation.js";
import type RequiredActionProviderRepresentation from "./requiredActionProviderRepresentation.js";
import type RolesRepresentation from "./rolesRepresentation.js";
import type ClientProfilesRepresentation from "./clientProfilesRepresentation.js";
import type ClientPoliciesRepresentation from "./clientPoliciesRepresentation.js";
import type RoleRepresentation from "./roleRepresentation.js";

/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_realmrepresentation
 */

export default interface RealmRepresentation {
  accessCodeLifespan?: number;
  accessCodeLifespanLogin?: number;
  accessCodeLifespanUserAction?: number;
  accessTokenLifespan?: number;
  accessTokenLifespanForImplicitFlow?: number;
  accountTheme?: string;
  actionTokenGeneratedByAdminLifespan?: number;
  actionTokenGeneratedByUserLifespan?: number;
  adminEventsDetailsEnabled?: boolean;
  adminEventsEnabled?: boolean;
  adminPermissionsClient?: ClientRepresentation;
  adminPermissionsEnabled?: boolean;
  adminTheme?: string;
  attributes?: Record<string, any>;
  // AuthenticationFlowRepresentation
  authenticationFlows?: any[];
  // AuthenticatorConfigRepresentation
  authenticatorConfig?: any[];
  browserFlow?: string;
  browserSecurityHeaders?: Record<string, any>;
  bruteForceProtected?: boolean;
  clientAuthenticationFlow?: string;
  clientScopeMappings?: Record<string, any>;
  // ClientScopeRepresentation
  clientScopes?: any[];
  clients?: ClientRepresentation[];
  clientPolicies?: ClientPoliciesRepresentation;
  clientProfiles?: ClientProfilesRepresentation;
  components?: { [index: string]: ComponentExportRepresentation };
  defaultDefaultClientScopes?: string[];
  defaultGroups?: string[];
  defaultLocale?: string;
  defaultOptionalClientScopes?: string[];
  defaultRoles?: string[];
  defaultRole?: RoleRepresentation;
  defaultSignatureAlgorithm?: string;
  directGrantFlow?: string;
  displayName?: string;
  displayNameHtml?: string;
  dockerAuthenticationFlow?: string;
  duplicateEmailsAllowed?: boolean;
  editUsernameAllowed?: boolean;
  emailTheme?: string;
  enabled?: boolean;
  enabledEventTypes?: string[];
  eventsEnabled?: boolean;
  eventsExpiration?: number;
  eventsListeners?: string[];
  failureFactor?: number;
  federatedUsers?: UserRepresentation[];
  groups?: GroupRepresentation[];
  id?: string;
  // IdentityProviderMapperRepresentation
  identityProviderMappers?: any[];
  identityProviders?: IdentityProviderRepresentation[];
  internationalizationEnabled?: boolean;
  keycloakVersion?: string;
  loginTheme?: string;
  loginWithEmailAllowed?: boolean;
  maxDeltaTimeSeconds?: number;
  maxFailureWaitSeconds?: number;
  maxTemporaryLockouts?: number;
  bruteForceStrategy?: "MULTIPLE" | "LINEAR";
  minimumQuickLoginWaitSeconds?: number;
  notBefore?: number;
  oauth2DeviceCodeLifespan?: number;
  oauth2DevicePollingInterval?: number;
  offlineSessionIdleTimeout?: number;
  offlineSessionMaxLifespan?: number;
  offlineSessionMaxLifespanEnabled?: boolean;
  organizationsEnabled?: boolean;
  verifiableCredentialsEnabled?: boolean;
  otpPolicyAlgorithm?: string;
  otpPolicyDigits?: number;
  otpPolicyInitialCounter?: number;
  otpPolicyLookAheadWindow?: number;
  otpPolicyPeriod?: number;
  otpPolicyType?: string;
  otpSupportedApplications?: string[];
  otpPolicyCodeReusable?: boolean;
  passwordPolicy?: string;
  permanentLockout?: boolean;
  // ProtocolMapperRepresentation
  protocolMappers?: any[];
  quickLoginCheckMilliSeconds?: number;
  realm?: string;
  refreshTokenMaxReuse?: number;
  registrationAllowed?: boolean;
  registrationEmailAsUsername?: boolean;
  registrationFlow?: string;
  rememberMe?: boolean;
  requiredActions?: RequiredActionProviderRepresentation[];
  resetCredentialsFlow?: string;
  resetPasswordAllowed?: boolean;
  revokeRefreshToken?: boolean;
  roles?: RolesRepresentation;
  // ScopeMappingRepresentation
  scopeMappings?: any[];
  smtpServer?: Record<string, any>;
  sslRequired?: string;
  ssoSessionIdleTimeout?: number;
  ssoSessionIdleTimeoutRememberMe?: number;
  ssoSessionMaxLifespan?: number;
  ssoSessionMaxLifespanRememberMe?: number;
  clientSessionIdleTimeout?: number;
  clientSessionMaxLifespan?: number;
  clientOfflineSessionIdleTimeout?: number;
  clientOfflineSessionMaxLifespan?: number;
  supportedLocales?: string[];
  // UserFederationMapperRepresentation
  userFederationMappers?: any[];
  // UserFederationProviderRepresentation
  userFederationProviders?: any[];
  userManagedAccessAllowed?: boolean;
  users?: UserRepresentation[];
  verifyEmail?: boolean;
  waitIncrementSeconds?: number;
}

export type PartialImportRealmRepresentation = RealmRepresentation & {
  ifResourceExists: "FAIL" | "SKIP" | "OVERWRITE";
};

export type PartialImportResponse = {
  overwritten: number;
  added: number;
  skipped: number;
  results: PartialImportResult[];
};

export type PartialImportResult = {
  action: string;
  resourceType: string;
  resourceName: string;
  id: string;
};
