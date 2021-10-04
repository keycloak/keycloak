/** @type {import("eslint").Linter.Config } */
module.exports = {
  root: true,
  ignorePatterns: ["node_modules", "build", "keycloak-theme", "server"],
  parserOptions: {
    project: "./tsconfig.eslint.json",
    extraFileExtensions: [".mjs"],
  },
  env: {
    node: true,
  },
  extends: [
    "eslint:recommended",
    "plugin:react/recommended",
    "plugin:@typescript-eslint/base",
    "plugin:@typescript-eslint/eslint-recommended",
    "plugin:prettier/recommended",
  ],
  settings: {
    react: {
      version: "detect",
    },
  },
  rules: {
    // Prefer using `includes()` to check if values exist over `indexOf() === -1`, as it's a more appropriate API for this.
    "@typescript-eslint/prefer-includes": "error",
    // Prefer using an optional chain expression, as it's more concise and easier to read.
    "@typescript-eslint/prefer-optional-chain": "error",
    "no-unused-vars": "off",
    "@typescript-eslint/no-empty-function": "error",
    "@typescript-eslint/no-unnecessary-condition": "warn",
    "@typescript-eslint/no-unused-vars": "error",
    // react/prop-types cannot handle generic props, so we need to disable it.
    // https://github.com/yannickcr/eslint-plugin-react/issues/2777#issuecomment-814968432
    "react/prop-types": "off",
    // Prevent fragments from being added that have only a single child.
    "react/jsx-no-useless-fragment": "error",
    "prefer-arrow-callback": "error",
    "prettier/prettier": [
      "error",
      {
        endOfLine: "auto",
      },
    ],
  },
  overrides: [
    {
      files: ["*.test.*"],
      rules: {
        // For tests it can make sense to pass empty functions as mocks.
        "@typescript-eslint/no-empty-function": "off",
      },
    },
    {
      files: ["cypress/**/*"],
      extends: ["plugin:cypress/recommended"],
      // TODO: Set these rules to "error" when issues have been resolved.
      rules: {
        "cypress/no-unnecessary-waiting": "warn",
      },
    },
  ],
};
