<h1 align="center">TypeScript ESLint Parser</h1>

<p align="center">An ESLint custom parser which leverages <a href="https://github.com/typescript-eslint/typescript-eslint/tree/master/packages/typescript-estree">TypeScript ESTree</a> to allow for ESLint to lint TypeScript source code.</p>

<p align="center">
    <a href="https://dev.azure.com/typescript-eslint/TypeScript%20ESLint/_build/latest?definitionId=1&branchName=master"><img src="https://img.shields.io/azure-devops/build/typescript-eslint/TypeScript%20ESLint/1/master.svg?label=%F0%9F%9A%80%20Azure%20Pipelines&style=flat-square" alt="Azure Pipelines"/></a>
    <a href="https://github.com/typescript-eslint/typescript-eslint/blob/master/LICENSE"><img src="https://img.shields.io/npm/l/typescript-estree.svg?style=flat-square" alt="GitHub license" /></a>
    <a href="https://www.npmjs.com/package/@typescript-eslint/parser"><img src="https://img.shields.io/npm/v/@typescript-eslint/parser.svg?style=flat-square" alt="NPM Version" /></a>
    <a href="https://www.npmjs.com/package/@typescript-eslint/parser"><img src="https://img.shields.io/npm/dm/@typescript-eslint/parser.svg?style=flat-square" alt="NPM Downloads" /></a>
    <a href="http://commitizen.github.io/cz-cli/"><img src="https://img.shields.io/badge/commitizen-friendly-brightgreen.svg?style=flat-square" alt="Commitizen friendly" /></a>
</p>

## Installation:

```sh
npm install @typescript-eslint/parser --save-dev
```

## Usage

In your ESLint configuration file, set the `parser` property:

```json
{
  "parser": "@typescript-eslint/parser"
}
```

There is sometimes an incorrect assumption that the parser itself is what does everything necessary to facilitate the use of ESLint with TypeScript. In actuality, it is the combination of the parser _and_ one or more plugins which allow you to maximize your usage of ESLint with TypeScript.

For example, once this parser successfully produces an AST for the TypeScript source code, it might well contain some information which simply does not exist in a standard JavaScript context, such as the data for a TypeScript-specific construct, like an `interface`.

The core rules built into ESLint, such as `indent` have no knowledge of such constructs, so it is impossible to expect them to work out of the box with them.

Instead, you also need to make use of one more plugins which will add or extend rules with TypeScript-specific features.

By far the most common case will be installing the [@typescript-eslint/eslint-plugin](https://github.com/typescript-eslint/typescript-eslint/tree/master/packages/eslint-plugin) plugin, but there are also other relevant options available such a [@typescript-eslint/eslint-plugin-tslint](https://github.com/typescript-eslint/typescript-eslint/tree/master/packages/eslint-plugin-tslint).

## Configuration

The following additional configuration options are available by specifying them in [`parserOptions`](https://eslint.org/docs/user-guide/configuring#specifying-parser-options) in your ESLint configuration file.

- **`ecmaFeatures.jsx`** - default `false`. Enable parsing JSX when `true`. More details can be found [here](https://www.typescriptlang.org/docs/handbook/jsx.html).

  - It's `false` on `*.ts` files regardless of this option.
  - It's `true` on `*.tsx` files regardless of this option.
  - Otherwise, it respects this option.

- **`useJSXTextNode`** - default `true`. Please set `false` if you use this parser on ESLint v4. If this is `false`, the parser creates the AST of JSX texts as the legacy style.

- **`project`** - default `undefined`. This option allows you to provide a path to your project's `tsconfig.json`. **This setting is required if you want to use rules which require type information**. You may want to use this setting in tandem with the `tsconfigRootDir` option below.

- **`tsconfigRootDir`** - default `undefined`. This option allows you to provide the root directory for relative tsconfig paths specified in the `project` option above.

- **`extraFileExtensions`** - default `undefined`. This option allows you to provide one or more additional file extensions which should be considered in the TypeScript Program compilation. E.g. a `.vue` file

- **`warnOnUnsupportedTypeScriptVersion`** - default `true`. This option allows you to toggle the warning that the parser will give you if you use a version of TypeScript which is not explicitly supported

### .eslintrc.json

```json
{
  "parser": "@typescript-eslint/parser",
  "parserOptions": {
    "ecmaFeatures": {
      "jsx": true
    },
    "useJSXTextNode": true,
    "project": "./tsconfig.json",
    "tsconfigRootDir": "../../",
    "extraFileExtensions": [".vue"]
  }
}
```

## Supported TypeScript Version

Please see https://github.com/typescript-eslint/typescript-eslint for the supported TypeScript version.

**Please ensure that you are using a supported version before submitting any issues/bug reports.**

## Reporting Issues

Please use the @typescript-eslint/parser issue template when creating your issue and fill out the information requested as best you can. This will really help us when looking into your issue.

## License

TypeScript ESLint Parser is licensed under a permissive BSD 2-clause license.
