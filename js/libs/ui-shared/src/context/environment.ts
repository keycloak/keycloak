/** The base environment variables that are shared between the Admin and Account Consoles. */
export type BaseEnvironment = {
  /**
   * The URL to the root of the Keycloak server, this is **NOT** always equivalent to the URL of the Admin Console.
   * For example, the Keycloak server could be hosted on `auth.example.com` and Admin Console may be hosted on `admin.example.com`.
   *
   * @see {@link https://www.keycloak.org/server/hostname#_administration_console}
   */
  authServerUrl: string;
  /** The identifier of the realm used to authenticate the user. */
  realm: string;
  /** The identifier of the client used to authenticate the user. */
  clientId: string;
  /** The base URL of the resources. */
  resourceUrl: string;
  /** The source URL for the the logo image. */
  logo: string;
  /** The URL to be followed when the logo is clicked. */
  logoUrl: string;
};

/**
 * Extracts the environment variables that are passed if the application is running as a Keycloak theme and combines them with the provided defaults.
 * These variables are injected by Keycloak into the `index.ftl` as a script tag, the contents of which can be parsed as JSON.
 *
 * @argument defaults - The default values to fall to if a value is not present in the environment.
 */
export function getInjectedEnvironment<T>(defaults: T): T {
  const element = document.getElementById("environment");
  let env = {} as T;

  // Attempt to parse the contents as JSON and return its value.
  try {
    // If the element cannot be found, return an empty record.
    if (element?.textContent) {
      env = JSON.parse(element.textContent);
    }
  } catch (error) {
    console.error("Unable to parse environment variables.");
  }

  // Return the merged environment variables with the defaults.
  return { ...defaults, ...env };
}
