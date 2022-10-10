export type Environment = {
  /** The URL to resources such as the files in the `public` directory. */
  resourceUrl: string;
};

// The default environment, used during development.
const defaultEnvironment: Environment = {
  resourceUrl: "http://localhost:8080",
};

export { defaultEnvironment as environment };
