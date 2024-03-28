import { matchPath } from "react-router-dom";
import { DEFAULT_REALM, ROOT_PATH } from "./constants";

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
  /** The locale of the user */
  locale: string;
  /** Feature flags */
  features: Feature;
  /** Client id of the application to add back link */
  referrer?: string;
  /** URI to the referrer application in the back link */
  referrer_uri?: string;
};

// Detect the current realm from the URL.
const match = matchPath(ROOT_PATH, location.pathname);

const defaultEnvironment: Environment = {
  authUrl: "http://localhost:8180",
  baseUrl: `http://localhost:8180/realms/${match?.params.realm ?? DEFAULT_REALM}/account/`,
  realm: match?.params.realm ?? DEFAULT_REALM,
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

  const searchParams = new URLSearchParams(location.search);
  if (searchParams.has("referrer_uri")) {
    env["referrer_uri"] = searchParams.get("referrer_uri")!;
  }
  if (searchParams.has("referrer")) {
    env["referrer"] = searchParams.get("referrer")!;
  }

  // Otherwise, return an empty record.
  return env;
}
