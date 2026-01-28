import {
  getInjectedEnvironment,
  type BaseEnvironment,
} from "@keycloak/keycloak-ui-shared";

export type Environment = BaseEnvironment & {
  /**
   * The URL to the root of the Administration Console, including the path if present, this takes into account the configured hostname of the Administration Console.
   * For example, the Keycloak server could be hosted on `auth.example.com` and Admin Console may be hosted on `admin.example.com/some/path`.
   *
   * Note that this URL is normalized not to include a trailing slash, so take this into account when constructing URLs.
   *
   * @see {@link https://www.keycloak.org/server/hostname#_administration_console}
   */
  adminBaseUrl: string;
  /** The URL to the base of the Admin Console. */
  consoleBaseUrl: string;
  /** The name of the master realm. */
  masterRealm: string;
  /** The version hash of the auth server. */
  resourceVersion: string;
};

export const environment = getInjectedEnvironment<Environment>();
