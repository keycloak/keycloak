export interface RealmRepresentation {
  id: string;
  realm: string;
  displayName?: string;
  displayNameHtml?: string;
  notBefore?: number;
  defaultSignatureAlgorithm?: string;
  revokeRefreshToken?: boolean;
  refreshTokenMaxReuse?: number;
  accessTokenLifespan?: number;
  accessTokenLifespanForImplicitFlow?: number;
  ssoSessionIdleTimeout?: number;
  ssoSessionMaxLifespan?: number;
  ssoSessionIdleTimeoutRememberMe?: number;
  ssoSessionMaxLifespanRememberMe?: number;
  offlineSessionIdleTimeout?: number;
  offlineSessionMaxLifespanEnabled?: boolean;
  offlineSessionMaxLifespan?: number;
  clientSessionIdleTimeout?: number;
  clientSessionMaxLifespan?: number;
  clientOfflineSessionIdleTimeout?: number;
  clientOfflineSessionMaxLifespan?: number;
  accessCodeLifespan?: number;
  accessCodeLifespanUserAction?: number;
  accessCodeLifespanLogin?: number;
  actionTokenGeneratedByAdminLifespan?: number;
  actionTokenGeneratedByUserLifespan?: number;
  enabled?: boolean;
  sslRequired?: string;
  passwordCredentialGrantAllowed?: boolean;
  registrationAllowed?: boolean;
  registrationEmailAsUsername?: boolean;
  rememberMe?: boolean;
  verifyEmail?: boolean;
  loginWithEmailAllowed?: boolean;
  duplicateEmailsAllowed?: boolean;
  resetPasswordAllowed?: boolean;
  editUsernameAllowed?: boolean;
  bruteForceProtected?: boolean;
  permanentLockout?: boolean;
  maxFailureWaitSeconds?: number;
  minimumQuickLoginWaitSeconds?: number;
  waitIncrementSeconds?: number;
  quickLoginCheckMilliSeconds?: number;
  maxDeltaTimeSeconds?: number;
  failureFactor?: number;
  privateKey?: string;
  publicKey?: string;
  certificate?: string;
  codeSecret?: string;
  roles?: RolesRepresentation;
  groups?: GroupRepresentation[];
  defaultRoles?: string[];
  defaultGroups?: string[];
  requiredCredentials?: string[];
  passwordPolicy?: string;
  otpPolicyType?: string;
  otpPolicyAlgorithm?: string;
  otpPolicyInitialCounter?: number;
  otpPolicyDigits?: number;
  otpPolicyLookAheadWindow?: number;
  otpPolicyPeriod?: number;
  otpSupportedApplications?: string[];
  webAuthnPolicyRpEntityName?: string;
  webAuthnPolicySignatureAlgorithms?: string[];
  webAuthnPolicyRpId?: string;
  webAuthnPolicyAttestationConveyancePreference?: string;
  webAuthnPolicyAuthenticatorAttachment?: string;
  webAuthnPolicyRequireResidentKey?: string;
  webAuthnPolicyUserVerificationRequirement?: string;
  webAuthnPolicyCreateTimeout?: number;
  webAuthnPolicyAvoidSameAuthenticatorRegister?: boolean;
  webAuthnPolicyAcceptableAaguids?: string[];
  webAuthnPolicyPasswordlessRpEntityName?: string;
  webAuthnPolicyPasswordlessSignatureAlgorithms?: string[];
  webAuthnPolicyPasswordlessRpId?: string;
  webAuthnPolicyPasswordlessAttestationConveyancePreference?: string;
  webAuthnPolicyPasswordlessAuthenticatorAttachment?: string;
  webAuthnPolicyPasswordlessRequireResidentKey?: string;
  webAuthnPolicyPasswordlessUserVerificationRequirement?: string;
  webAuthnPolicyPasswordlessCreateTimeout?: number;
  webAuthnPolicyPasswordlessAvoidSameAuthenticatorRegister?: boolean;
  webAuthnPolicyPasswordlessAcceptableAaguids?: string[];
  users?: UserRepresentation[];
  federatedUsers?: UserRepresentation[];
  scopeMappings?: ScopeMappingRepresentation[];
  clientScopeMappings?: { [index: string]: ScopeMappingRepresentation[] };
  clients?: ClientRepresentation[];
  clientScopes?: ClientScopeRepresentation[];
  defaultDefaultClientScopes?: string[];
  defaultOptionalClientScopes?: string[];
  browserSecurityHeaders?: { [index: string]: string };
  smtpServe?: { [index: string]: string };
  userFederationProviders?: UserFederationProviderRepresentation[];
  userFederationMappers?: UserFederationMapperRepresentation[];
  loginTheme?: string;
  accountTheme?: string;
  adminTheme?: string;
  emailTheme?: string;
  eventsEnabled?: boolean;
  eventsExpiration?: number;
  eventsListeners?: string[];
  enabledEventTypes?: string[];
  adminEventsEnabled?: boolean;
  adminEventsDetailsEnabled?: boolean;
  identityProviders?: IdentityProviderRepresentation[];
  identityProviderMappers?: IdentityProviderMapperRepresentation[];
  protocolMappers?: ProtocolMapperRepresentation[];
  components?: { [index: string]: ComponentExportRepresentation };
  internationalizationEnabled?: boolean;
  supportedLocales?: string[];
  defaultLocale?: string;
  authenticationFlows?: AuthenticationFlowRepresentation[];
  authenticatorConfig?: AuthenticatorConfigRepresentation[];
  requiredActions?: RequiredActionProviderRepresentation[];
  browserFlow?: string;
  registrationFlow?: string;
  directGrantFlow?: string;
  resetCredentialsFlow?: string;
  clientAuthenticationFlow?: string;
  dockerAuthenticationFlow?: string;
  attributes?: { [index: string]: string };
  keycloakVersion?: string;
  userManagedAccessAllowed?: boolean;
  social?: boolean;
  updateProfileOnInitialSocialLogin?: boolean;
  socialProviders?: { [index: string]: string };
  applicationScopeMappings?: { [index: string]: ScopeMappingRepresentation[] };
  applications?: ApplicationRepresentation[];
  oauthClients?: OAuthClientRepresentation[];
  clientTemplates?: ClientTemplateRepresentation[];
}

export interface RolesRepresentation {
  realm: RoleRepresentation[];
  client: { [index: string]: RoleRepresentation[] };
  application: { [index: string]: RoleRepresentation[] };
}

export interface GroupRepresentation {
  id: string;
  name: string;
  path: string;
  attributes: { [index: string]: string[] };
  realmRoles: string[];
  clientRoles: { [index: string]: string[] };
  subGroups: GroupRepresentation[];
  access: { [index: string]: boolean };
}

export interface UserRepresentation {
  self: string;
  id: string;
  origin: string;
  createdTimestamp: number;
  username: string;
  enabled: boolean;
  totp: boolean;
  emailVerified: boolean;
  firstName: string;
  lastName: string;
  email: string;
  federationLink: string;
  serviceAccountClientId: string;
  attributes: { [index: string]: string[] };
  credentials: CredentialRepresentation[];
  disableableCredentialTypes: string[];
  requiredActions: string[];
  federatedIdentities: FederatedIdentityRepresentation[];
  realmRoles: string[];
  clientRoles: { [index: string]: string[] };
  clientConsents: UserConsentRepresentation[];
  notBefore: number;
  applicationRoles: { [index: string]: string[] };
  socialLinks: SocialLinkRepresentation[];
  groups: string[];
  access: { [index: string]: boolean };
}

export interface ScopeMappingRepresentation {
  self: string;
  client: string;
  clientTemplate: string;
  clientScope: string;
  roles: string[];
}

export interface ClientRepresentation {
  id: string;
  clientId: string;
  name: string;
  description: string;
  rootUrl: string;
  adminUrl: string;
  baseUrl: string;
  surrogateAuthRequired: boolean;
  enabled: boolean;
  alwaysDisplayInConsole: boolean;
  clientAuthenticatorType: string;
  secret: string;
  registrationAccessToken: string;
  defaultRoles: string[];
  redirectUris: string[];
  webOrigins: string[];
  notBefore: number;
  bearerOnly: boolean;
  consentRequired: boolean;
  standardFlowEnabled: boolean;
  implicitFlowEnabled: boolean;
  directAccessGrantsEnabled: boolean;
  serviceAccountsEnabled: boolean;
  authorizationServicesEnabled: boolean;
  directGrantsOnly: boolean;
  publicClient: boolean;
  frontchannelLogout: boolean;
  protocol: string;
  attributes: { [index: string]: string };
  authenticationFlowBindingOverrides: { [index: string]: string };
  fullScopeAllowed: boolean;
  nodeReRegistrationTimeout: number;
  registeredNodes: { [index: string]: number };
  protocolMappers: ProtocolMapperRepresentation[];
  clientTemplate: string;
  useTemplateConfig: boolean;
  useTemplateScope: boolean;
  useTemplateMappers: boolean;
  defaultClientScopes: string[];
  optionalClientScopes: string[];
  authorizationSettings: ResourceServerRepresentation;
  access: { [index: string]: boolean };
  origin: string;
}

export interface ClientScopeRepresentation {
  id: string;
  name: string;
  description: string;
  protocol: string;
  attributes: { [index: string]: string };
  protocolMappers: ProtocolMapperRepresentation[];
}

export interface UserFederationProviderRepresentation {
  id: string;
  displayName: string;
  providerName: string;
  config: { [index: string]: string };
  priority: number;
  fullSyncPeriod: number;
  changedSyncPeriod: number;
  lastSync: number;
}

export interface UserFederationMapperRepresentation {
  id: string;
  name: string;
  federationProviderDisplayName: string;
  federationMapperType: string;
  config: { [index: string]: string };
}

export interface IdentityProviderRepresentation {
  alias: string;
  displayName: string;
  internalId: string;
  providerId: string;
  enabled: boolean;
  updateProfileFirstLoginMode: string;
  trustEmail: boolean;
  storeToken: boolean;
  addReadTokenRoleOnCreate: boolean;
  authenticateByDefault: boolean;
  linkOnly: boolean;
  firstBrokerLoginFlowAlias: string;
  postBrokerLoginFlowAlias: string;
  config: { [index: string]: string };
}

export interface IdentityProviderMapperRepresentation {
  id: string;
  name: string;
  identityProviderAlias: string;
  identityProviderMapper: string;
  config: { [index: string]: string };
}

export interface ProtocolMapperRepresentation {
  id: string;
  name: string;
  protocol: string;
  protocolMapper: string;
  consentRequired: boolean;
  consentText: string;
  config: { [index: string]: string };
}

export interface ComponentExportRepresentation {
  id: string;
  name: string;
  providerId: string;
  subType: string;
  subComponents: { [index: string]: ComponentExportRepresentation };
  config: { [index: string]: string };
}

export interface AuthenticationFlowRepresentation extends Serializable {
  id: string;
  alias: string;
  description: string;
  providerId: string;
  topLevel: boolean;
  builtIn: boolean;
  authenticationExecutions: AuthenticationExecutionExportRepresentation[];
}

export interface AuthenticatorConfigRepresentation extends Serializable {
  id: string;
  alias: string;
  config: { [index: string]: string };
}

export interface RequiredActionProviderRepresentation {
  alias: string;
  name: string;
  providerId: string;
  enabled: boolean;
  defaultAction: boolean;
  priority: number;
  config: { [index: string]: string };
}

export interface ApplicationRepresentation extends ClientRepresentation {
  claims: ClaimRepresentation;
}

export interface OAuthClientRepresentation extends ApplicationRepresentation {}

export interface ClientTemplateRepresentation {
  id: string;
  name: string;
  description: string;
  protocol: string;
  fullScopeAllowed: boolean;
  bearerOnly: boolean;
  consentRequired: boolean;
  standardFlowEnabled: boolean;
  implicitFlowEnabled: boolean;
  directAccessGrantsEnabled: boolean;
  serviceAccountsEnabled: boolean;
  publicClient: boolean;
  frontchannelLogout: boolean;
  attributes: { [index: string]: string };
  protocolMappers: ProtocolMapperRepresentation[];
}

export interface RoleRepresentation {
  id: string;
  name: string;
  description: string;
  scopeParamRequired: boolean;
  composite: boolean;
  composites: Composites;
  clientRole: boolean;
  containerId: string;
  attributes: { [index: string]: string[] };
}

export interface CredentialRepresentation {
  id: string;
  type: string;
  userLabel: string;
  createdDate: number;
  secretData: string;
  credentialData: string;
  priority: number;
  value: string;
  temporary: boolean;
  device: string;
  hashedSaltedValue: string;
  salt: string;
  hashIterations: number;
  counter: number;
  algorithm: string;
  digits: number;
  period: number;
  config: { [index: string]: string };
}

export interface FederatedIdentityRepresentation {
  identityProvider: string;
  userId: string;
  userName: string;
}

export interface UserConsentRepresentation {
  clientId: string;
  grantedClientScopes: string[];
  createdDate: number;
  lastUpdatedDate: number;
  grantedRealmRoles: string[];
}

export interface SocialLinkRepresentation {
  socialProvider: string;
  socialUserId: string;
  socialUsername: string;
}

export interface ResourceServerRepresentation {
  id: string;
  clientId: string;
  name: string;
  allowRemoteResourceManagement: boolean;
  policyEnforcementMode: PolicyEnforcementMode;
  resources: ResourceRepresentation[];
  policies: PolicyRepresentation[];
  scopes: ScopeRepresentation[];
  decisionStrategy: DecisionStrategy;
}

export interface AuthenticationExecutionExportRepresentation
  extends AbstractAuthenticationExecutionRepresentation {
  flowAlias: string;
  userSetupAllowed: boolean;
}

export interface Serializable {}

export interface ClaimRepresentation {
  name: boolean;
  username: boolean;
  profile: boolean;
  picture: boolean;
  website: boolean;
  email: boolean;
  gender: boolean;
  locale: boolean;
  address: boolean;
  phone: boolean;
}

export interface Composites {
  realm: string[];
  client: { [index: string]: string[] };
  application: { [index: string]: string[] };
}

export interface ResourceRepresentation {
  name: string;
  type: string;
  owner: ResourceOwnerRepresentation;
  ownerManagedAccess: boolean;
  displayName: string;
  attributes: { [index: string]: string[] };
  _id: string;
  uris: string[];
  scopes: ScopeRepresentation[];
  icon_uri: string;
}

export interface PolicyRepresentation extends AbstractPolicyRepresentation {
  config: { [index: string]: string };
}

export interface ScopeRepresentation {
  id: string;
  name: string;
  iconUri: string;
  policies: PolicyRepresentation[];
  resources: ResourceRepresentation[];
  displayName: string;
}

export interface AbstractAuthenticationExecutionRepresentation
  extends Serializable {
  authenticatorConfig: string;
  authenticator: string;
  requirement: string;
  priority: number;
  autheticatorFlow: boolean;
}

export interface ResourceOwnerRepresentation {
  id: string;
  name: string;
}

export interface AbstractPolicyRepresentation {
  id: string;
  name: string;
  description: string;
  type: string;
  policies: string[];
  resources: string[];
  scopes: string[];
  logic: Logic;
  decisionStrategy: DecisionStrategy;
  owner: string;
  resourcesData: ResourceRepresentation[];
  scopesData: ScopeRepresentation[];
}

export type PolicyEnforcementMode = "ENFORCING" | "PERMISSIVE" | "DISABLED";

export type DecisionStrategy = "AFFIRMATIVE" | "UNANIMOUS" | "CONSENSUS";

export type Logic = "POSITIVE" | "NEGATIVE";
