export interface Client {
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

  publicClient: boolean;
  frontchannelLogout: boolean;
  protocol: string;
  attributes: Map<string, string>;
  authenticationFlowBindingOverrides: Map<string, string>;
  fullScopeAllowed: boolean;
  nodeReRegistrationTimeout: number;
  registeredNodes: Map<string, number>;
  //protocolMappers: ProtocolMapperRepresentation[];
}
