import {
  getInjectedEnvironment,
  type BaseEnvironment,
} from "@keycloak/keycloak-ui-shared";

export type Environment = BaseEnvironment & {
  /** The URL to the root of the account console. */
  baseUrl: string;
  /** The locale of the user */
  locale: string;
  /** Name of the referrer application in the back link */
  referrerName?: string;
  /** UR to the referrer application in the back link */
  referrerUrl?: string;
  /** Feature flags */
  features: Feature;
};

export type Feature = {
  isRegistrationEmailAsUsername: boolean;
  isEditUserNameAllowed: boolean;
  isLinkedAccountsEnabled: boolean;
  isMyResourcesEnabled: boolean;
  deleteAccountAllowed: boolean;
  updateEmailFeatureEnabled: boolean;
  updateEmailActionEnabled: boolean;
  isViewGroupsEnabled: boolean;
  isOid4VciEnabled: boolean;
};

// During development the realm can be passed as a query parameter when redirecting back from Keycloak.
const realm =
  new URLSearchParams(window.location.search).get("realm") ||
  location.pathname.match("/realms/(.*?)/account")?.[1] ||
  "master";

const defaultEnvironment: Environment = {
  // Base environment variables
  authServerUrl: "http://localhost:8180",
  realm: realm,
  clientId: "security-admin-console-v2",
  resourceUrl: "http://localhost:8080",
  logo: "/logo.svg",
  logoUrl: "/",
  // Account Console specific environment variables
  baseUrl: `http://localhost:8180/realms/${realm}/account/`,
  locale: "en",
  features: {
    isRegistrationEmailAsUsername: false,
    isEditUserNameAllowed: true,
    isLinkedAccountsEnabled: true,
    isMyResourcesEnabled: true,
    deleteAccountAllowed: true,
    updateEmailFeatureEnabled: true,
    updateEmailActionEnabled: true,
    isViewGroupsEnabled: true,
    isOid4VciEnabled: true,
  },
};

export const environment = getInjectedEnvironment(defaultEnvironment);
