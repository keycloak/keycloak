/**
 * https://www.keycloak.org/docs-api/11.0/rest-api/index.html#_requiredactionproviderrepresentation
 */

export enum RequiredActionAlias {
  VERIFY_EMAIL = "VERIFY_EMAIL",
  UPDATE_PROFILE = "UPDATE_PROFILE",
  CONFIGURE_TOTP = "CONFIGURE_TOTP",
  UPDATE_PASSWORD = "UPDATE_PASSWORD",
  TERMS_AND_CONDITIONS = "TERMS_AND_CONDITIONS",
}

export default interface RequiredActionProviderRepresentation {
  alias?: string;
  config?: Record<string, any>;
  defaultAction?: boolean;
  enabled?: boolean;
  name?: string;
  providerId?: string;
  priority?: number;
}
