<h1 align="center">ESLint Plugin TypeScript</h1>

<p align="center">An ESLint plugin which provides lint rules for TypeScript codebases.</p>

<p align="center">
    <img src="https://github.com/typescript-eslint/typescript-eslint/workflows/CI/badge.svg" alt="CI" />
    <a href="https://www.npmjs.com/package/@typescript-eslint/eslint-plugin"><img src="https://img.shields.io/npm/v/@typescript-eslint/eslint-plugin.svg?style=flat-square" alt="NPM Version" /></a>
    <a href="https://www.npmjs.com/package/@typescript-eslint/eslint-plugin"><img src="https://img.shields.io/npm/dm/@typescript-eslint/eslint-plugin.svg?style=flat-square" alt="NPM Downloads" /></a>
</p>

## Getting Started

- **[You can find our Getting Started docs here](https://typescript-eslint.io/docs/linting)**
- **[You can find our FAQ / Troubleshooting docs here](https://typescript-eslint.io/docs/linting/troubleshooting)**

These docs walk you through setting up ESLint, this plugin, and our parser. If you know what you're doing and just want to quick start, read on...

## Quick-start

### Installation

Make sure you have TypeScript and [`@typescript-eslint/parser`](../parser) installed:

```bash
$ yarn add -D typescript @typescript-eslint/parser
$ npm i --save-dev typescript @typescript-eslint/parser
```

Then install the plugin:

```bash
$ yarn add -D @typescript-eslint/eslint-plugin
$ npm i --save-dev @typescript-eslint/eslint-plugin
```

It is important that you use the same version number for `@typescript-eslint/parser` and `@typescript-eslint/eslint-plugin`.

**Note:** If you installed ESLint globally (using the `-g` flag) then you must also install `@typescript-eslint/eslint-plugin` globally.

### Usage

Add `@typescript-eslint/parser` to the `parser` field and `@typescript-eslint` to the plugins section of your `.eslintrc` configuration file, then configure the rules you want to use under the rules section.

```json
{
  "parser": "@typescript-eslint/parser",
  "plugins": ["@typescript-eslint"],
  "rules": {
    "@typescript-eslint/rule-name": "error"
  }
}
```

You can also enable all the recommended rules for our plugin. Add `plugin:@typescript-eslint/recommended` in extends:

```json
{
  "extends": ["plugin:@typescript-eslint/recommended"]
}
```

### Recommended Configs

You can also use [`eslint:recommended`](https://eslint.org/docs/rules/) (the set of rules which are recommended for all projects by the ESLint Team) with this plugin:

```json
{
  "extends": ["eslint:recommended", "plugin:@typescript-eslint/recommended"]
}
```

As of version 2 of this plugin, _by design_, none of the rules in the main `recommended` config require type-checking in order to run. This means that they are more lightweight and faster to run.

Some highly valuable rules require type-checking in order to be implemented correctly, however, so we provide an additional config you can extend from called `recommended-requiring-type-checking`. You would apply this _in addition_ to the recommended configs previously mentioned, e.g.:

```json
{
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:@typescript-eslint/recommended-requiring-type-checking"
  ]
}
```

Pro Tip: For larger codebases you may want to consider splitting our linting into two separate stages: 1. fast feedback rules which operate purely based on syntax (no type-checking), 2. rules which are based on semantics (type-checking).

**[You can read more about linting with type information here](https://typescript-eslint.io/docs/linting/typed-linting)**

## Supported Rules

For the exhaustive list of supported rules, [please see our website](https://typescript-eslint.io/rules/).

## Contributing

[See the contributing guide here](../../CONTRIBUTING.md).
