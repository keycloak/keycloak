export interface ClientRepresentation {
  id?: string;
  clientId?: string;
  name?: string;
  description?: string;
  rootUrl?: string;
  adminUrl?: string;
  baseUrl?: string;
  surrogateAuthRequired?: boolean;
  enabled?: boolean;
  alwaysDisplayInConsole?: boolean;
  clientAuthenticatorType?: string;
  secret?: string;
  registrationAccessToken?: string;
  defaultRoles?: string[];
  redirectUris?: string[];
  webOrigins?: string[];
  notBefore?: number;
  bearerOnly?: boolean;
  consentRequired?: boolean;
  standardFlowEnabled?: boolean;
  implicitFlowEnabled?: boolean;
  directAccessGrantsEnabled?: boolean;
  serviceAccountsEnabled?: boolean;
  authorizationServicesEnabled?: boolean;
  directGrantsOnly?: boolean;
  publicClient?: boolean;
  frontchannelLogout?: boolean;
  protocol?: string;
  attributes?: { [index: string]: string };
  authenticationFlowBindingOverrides?: { [index: string]: string };
  fullScopeAllowed?: boolean;
  nodeReRegistrationTimeout?: number;
  registeredNodes?: { [index: string]: number };
  protocolMappers?: ProtocolMapperRepresentation[];
  clientTemplate?: string;
  useTemplateConfig?: boolean;
  useTemplateScope?: boolean;
  useTemplateMappers?: boolean;
  defaultClientScopes?: string[];
  optionalClientScopes?: string[];
  authorizationSettings?: ResourceServerRepresentation;
  access?: { [index: string]: boolean };
  origin?: string;
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
