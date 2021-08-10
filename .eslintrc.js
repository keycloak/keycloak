/** @type {import("eslint").Linter.Config } */
module.exports = {
  root: true,
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
    "no-unused-vars": "off",
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/no-empty-function": "error",
    // react/prop-types cannot handle generic props, so we need to disable it.
    // https://github.com/yannickcr/eslint-plugin-react/issues/2777#issuecomment-814968432
    "react/prop-types": "off",
  },
  overrides: [
    {
      files: ["*.test.*"],
      rules: {
        // For tests it can make sense to pass empty functions as mocks.
        "@typescript-eslint/no-empty-function": "off",
      },
    },
  ],
};
