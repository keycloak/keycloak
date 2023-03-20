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

export { defaultEnvironment as environment };
