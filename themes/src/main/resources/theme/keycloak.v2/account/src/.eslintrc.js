/** @type {import('eslint').Linter.Config } */
module.exports = {
  extends: [
    'react-app',
    'plugin:jsx-a11y/recommended',
    'plugin:react/recommended',
    'plugin:@typescript-eslint/recommended',
  ],
  settings: {
    react: {
      version: '16',
    }
  },
  rules: {
    '@typescript-eslint/ban-types': 'warn',
    '@typescript-eslint/no-empty-interface': 'warn',
    '@typescript-eslint/no-extra-semi': 'warn',
    'prefer-const': 'warn',
    'react/prop-types': 'warn',
  }
};
