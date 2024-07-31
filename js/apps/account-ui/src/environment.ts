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
};

export type Environment = {
  /** The URL to the root of the auth server. */
  authUrl: string;
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
  /** The locale of the user */
  locale: string;
  /** Feature flags */
  features: Feature;
  /** Name of the referrer application in the back link */
  referrerName?: string;
  /** UR to the referrer application in the back link */
  referrerUrl?: string;
};

// The default environment, used during development.
const realm = new URLSearchParams(window.location.search).get("realm");
const defaultEnvironment: Environment = {
  authUrl: "http://localhost:8180",
  realm: realm || "master",
  clientId: "security-admin-console-v2",
  resourceUrl: "http://localhost:8080",
  logo: "/logo.svg",
  logoUrl: "/",
  locale: "en",
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
  },
};

// Merge the default and injected environment variables together.
const environment: Environment = {
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

  // If the element cannot be found, return an empty record.
  if (!element?.textContent) {
    return {};
  }

  // Attempt to parse the contents as JSON and return its value.
  try {
    return JSON.parse(element.textContent);
  } catch (error) {
    console.error("Unable to parse environment variables.");
  }

  // Otherwise, return an empty record.
  return {};
}
