/** @type {import("eslint").Linter.Config } */
module.exports = {
  root: true,
  ignorePatterns: [
    "node_modules",
    "dist",
    "keycloak-theme",
    "server",
    // Keycloak JS follows a completely different and outdated style, so we'll exclude it for now.
    // TODO: Eventually align the code-style for Keycloak JS.
    "libs/keycloak-js",
  ],
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: "./tsconfig.eslint.json",
    extraFileExtensions: [".mjs"],
  },
  env: {
    node: true,
  },
  plugins: ["lodash"],
  extends: [
    "eslint:recommended",
    "plugin:import/recommended",
    "plugin:import/typescript",
    "plugin:react/recommended",
    "plugin:react/jsx-runtime",
    "plugin:react-hooks/recommended",
    "plugin:@typescript-eslint/base",
    "plugin:@typescript-eslint/eslint-recommended",
    "plugin:prettier/recommended",
  ],
  settings: {
    react: {
      version: "detect",
    },
    "import/resolver": {
      typescript: true,
      node: true,
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
    "lodash/import-scope": ["error", "member"],
    // react/prop-types cannot handle generic props, so we need to disable it.
    // https://github.com/yannickcr/eslint-plugin-react/issues/2777#issuecomment-814968432
    "react/prop-types": "off",
    // Prevent fragments from being added that have only a single child.
    "react/jsx-no-useless-fragment": "error",
    // Ban nesting components, as this will cause unintended re-mounting of components.
    // TODO: All issues should be fixed and this rule should be set to "error".
    "react/no-unstable-nested-components": ["warn", { allowAsProps: true }],
    "prefer-arrow-callback": "error",
    "prettier/prettier": [
      "error",
      {
        endOfLine: "auto",
      },
    ],
    // Prevent default imports from React, named imports should be used instead.
    "no-restricted-imports": [
      "error",
      {
        paths: [
          {
            name: "react",
            importNames: ["default"],
          },
        ],
      },
    ],
    // Prefer using the `#private` syntax for private class members, we want to keep this consistent and use the same syntax.
    "no-restricted-syntax": [
      "error",
      {
        selector:
          ':matches(PropertyDefinition, MethodDefinition)[accessibility="private"]',
        message: "Use #private instead",
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
      files: ["**/cypress/**/*"],
      extends: ["plugin:cypress/recommended", "plugin:mocha/recommended"],
      // TODO: Set these rules to "error" when issues have been resolved.
      rules: {
        "cypress/no-unnecessary-waiting": "warn",
        "cypress/unsafe-to-chain-command": "warn",
        "mocha/max-top-level-suites": "off",
        "mocha/no-exclusive-tests": "error",
        "mocha/no-identical-title": "off",
        "mocha/no-mocha-arrows": "off",
        "mocha/no-setup-in-describe": "off",
      },
    },
  ],
};
