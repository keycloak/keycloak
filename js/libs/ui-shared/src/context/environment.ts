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
 *  Extracts the environment variables from the document, these variables are injected by Keycloak as a script tag, the contents of which can be parsed as JSON.
 *
 * @argument defaults - The default values to fall to if a value is not found in the environment.
 */
export function getInjectedEnvironment<T>(defaults: T): T {
  const element = document.getElementById("environment");
  const contents = element?.textContent;

  if (typeof contents !== "string") {
    throw new Error("Environment variables not found in the document.");
  }

  try {
    return { ...defaults, ...JSON.parse(contents) };
  } catch (error) {
    throw new Error("Unable to parse environment variables as JSON.");
  }
}
