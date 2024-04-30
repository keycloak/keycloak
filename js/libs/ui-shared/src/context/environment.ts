export const DEFAULT_REALM = "master";

export type Feature = {
  isRegistrationEmailAsUsername: boolean;
  isEditUserNameAllowed: boolean;
  isInternationalizationEnabled: boolean;
  isLinkedAccountsEnabled: boolean;
  isEventsEnabled: boolean;
  isMyResourcesEnabled: boolean;
  isTotpConfigured: boolean;
  deleteAccountAllowed: boolean;
  updateEmailFeatureEnabled: boolean;
  updateEmailActionEnabled: boolean;
  isViewGroupsEnabled: boolean;
  isOid4VciEnabled: boolean;
};

export type BaseEnvironment = {
  /** The URL to the root of the auth server. */
  authUrl: string;
  /** The URL to the root of the account console. */
  baseUrl: string;
  /** The realm used to authenticate the user to the Account Console. */
  realm: string;
  /** The identifier of the client used to authenticate the user to the Account Console. */
  clientId: string;
  /** The URL to resources such as the files in the `public` directory. */
  resourceUrl: string;
  /** Indicates the src for the Brand image */
  logo: string;
  /** Indicates the url to be followed when Brand image is clicked */
  logoUrl: string;
};

export type AdminEnvironment = BaseEnvironment & {
  /** The URL to the root of the auth server. */
  authServerUrl: string;
  /** The name of the master realm. */
  masterRealm: string;
  /** The URL to the base of the Admin UI. */
  consoleBaseUrl: string;
  /** The version hash of the auth server. */
  resourceVersion: string;
};

export type AccountEnvironment = BaseEnvironment & {
  /** The locale of the user */
  locale: string;
  /** Feature flags */
  features: Feature;
  /** Name of the referrer application in the back link */
  referrerName?: string;
  /** UR to the referrer application in the back link */
  referrerUrl?: string;
};

// During development the realm can be passed as a query parameter when redirecting back from Keycloak.
const realm =
  new URLSearchParams(window.location.search).get("realm") ||
  location.pathname.match("/realms/(.*?)/account")?.[1];

const defaultEnvironment: AdminEnvironment & AccountEnvironment = {
  authUrl: "http://localhost:8180",
  authServerUrl: "http://localhost:8180",
  baseUrl: `http://localhost:8180/realms/${realm ?? DEFAULT_REALM}/account/`,
  realm: realm ?? DEFAULT_REALM,
  clientId: "security-admin-console-v2",
  resourceUrl: "http://localhost:8080",
  logo: "/logo.svg",
  logoUrl: "/",
  locale: "en",
  consoleBaseUrl: "/admin/master/console/",
  masterRealm: "master",
  resourceVersion: "unknown",
  features: {
    isRegistrationEmailAsUsername: false,
    isEditUserNameAllowed: true,
    isInternationalizationEnabled: true,
    isLinkedAccountsEnabled: true,
    isEventsEnabled: true,
    isMyResourcesEnabled: true,
    isTotpConfigured: true,
    deleteAccountAllowed: true,
    updateEmailFeatureEnabled: true,
    updateEmailActionEnabled: true,
    isViewGroupsEnabled: true,
    isOid4VciEnabled: false,
  },
};

// Merge the default and injected environment variables together.
const environment = {
  ...defaultEnvironment,
  ...getInjectedEnvironment(),
};

export { environment };

/**
 * Extracts the environment variables that are passed if the application is running as a Keycloak theme.
 * These variables are injected by Keycloak into the `index.ftl` as a script tag, the contents of which can be parsed as JSON.
 */
function getInjectedEnvironment(): Record<string, string | number | boolean> {
  const element = document.getElementById("environment");

  let env = {} as Record<string, string | number | boolean>;

  // Attempt to parse the contents as JSON and return its value.
  try {
    // If the element cannot be found, return an empty record.
    if (element?.textContent) {
      env = JSON.parse(element.textContent);
    }
  } catch (error) {
    console.error("Unable to parse environment variables.");
  }

  // Otherwise, return an empty record.
  return env;
}
