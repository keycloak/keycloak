export type Environment = {
  /** The realm which should be used when signing into the application. */
  loginRealm: string;
  /** The URL to the root of the auth server. */
  authServerUrl: string;
  /** The URL to resources such as the files in the `public` directory. */
  resourceUrl: string;
  /** Indicates if the application is running as a Keycloak theme. */
  isRunningAsTheme: boolean;
};

// The default environment, used during development.
const defaultEnvironment: Environment = {
  loginRealm: "master",
  authServerUrl: "http://localhost:8180",
  resourceUrl: "http://localhost:8080",
  isRunningAsTheme: false,
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
