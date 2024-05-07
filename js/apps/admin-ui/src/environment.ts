export type Environment = {
  /** The realm used to authenticate the user to the Admin Console. */
  loginRealm: string;
  /** The identifier of the client used to authenticate the user to the Admin Console. */
  clientId: string;
  /** The URL to the root of the auth server. */
  authServerUrl: string;
  /** The URL to the path of the auth server where client requests can be sent. */
  authUrl: string;
  /** The URL to the base of the Admin UI. */
  consoleBaseUrl: string;
  /** The URL to resources such as the files in the `public` directory. */
  resourceUrl: string;
  /** The name of the master realm. */
  masterRealm: string;
  /** The version hash of the auth server. */
  resourceVersion: string;
  /** Indicates the src for the Brand image */
  logo: string;
  /** Indicates the url to be followed when Brand image is clicked */
  logoUrl: string;
  /** Indicates the default role filter to present when mapping roles */
  defaultRoleMappingFilter: string;
};

// During development the realm can be passed as a query parameter when redirecting back from Keycloak.
const realm = new URLSearchParams(window.location.search).get("realm");

// The default environment, used during development.
const defaultEnvironment: Environment = {
  loginRealm: realm ?? "master",
  clientId: "security-admin-console-v2",
  authServerUrl: "http://localhost:8180",
  authUrl: "http://localhost:8180",
  consoleBaseUrl: "/admin/master/console/",
  resourceUrl: ".",
  masterRealm: "master",
  resourceVersion: "unknown",
  logo: "/logo.svg",
  logoUrl: "",
  defaultRoleMappingFilter: "",
};

// Merge the default and injected environment variables together.
const environment: Environment = {
  ...defaultEnvironment,
  ...getInjectedEnvironment(),
};

export default environment;

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
