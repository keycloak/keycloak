<h1 align="center">TypeScript ESTree</h1>

<p align="center">A parser that converts TypeScript source code into an <a href="https://github.com/estree/estree">ESTree</a>-compatible form</p>

<p align="center">
    <a href="https://dev.azure.com/typescript-eslint/TypeScript%20ESLint/_build/latest?definitionId=1&branchName=master"><img src="https://img.shields.io/azure-devops/build/typescript-eslint/TypeScript%20ESLint/1/master.svg?label=%F0%9F%9A%80%20Azure%20Pipelines&style=flat-square" alt="Azure Pipelines"/></a>
    <a href="https://github.com/typescript-eslint/typescript-eslint/blob/master/LICENSE"><img src="https://img.shields.io/npm/l/typescript-estree.svg?style=flat-square" alt="GitHub license" /></a>
    <a href="https://www.npmjs.com/package/@typescript-eslint/typescript-estree"><img src="https://img.shields.io/npm/v/@typescript-eslint/typescript-estree.svg?style=flat-square" alt="NPM Version" /></a>
    <a href="https://www.npmjs.com/package/@typescript-eslint/typescript-estree"><img src="https://img.shields.io/npm/dm/@typescript-eslint/typescript-estree.svg?style=flat-square" alt="NPM Downloads" /></a>
    <a href="http://commitizen.github.io/cz-cli/"><img src="https://img.shields.io/badge/commitizen-friendly-brightgreen.svg?style=flat-square" alt="Commitizen friendly" /></a>
</p>

<br>

## About

This parser is somewhat generic and robust, and could be used to power any use-case which requires taking TypeScript source code and producing an ESTree-compatible AST.

In fact, it is already used within these hyper-popular open-source projects to power their TypeScript support:

- [ESLint](https://eslint.org), the pluggable linting utility for JavaScript and JSX
- [Prettier](https://prettier.io), an opinionated code formatter

## Installation

```sh
npm install @typescript-eslint/typescript-estree --save-dev
```

## API

### parse(code, options)

Parses the given string of code with the options provided and returns an ESTree-compatible AST. The options object has the following properties:

```js
{
  // attach range information to each node
  range: false,

  // attach line/column location information to each node
  loc: false,

  // create a top-level tokens array containing all tokens
  tokens: false,

  // create a top-level comments array containing all comments
  comment: false,

  // enable parsing JSX. For more details, see https://www.typescriptlang.org/docs/handbook/jsx.html
  jsx: false,

  /*
   * The JSX AST changed the node type for string literals
   * inside a JSX Element from `Literal` to `JSXText`.
   * When value is `true`, these nodes will be parsed as type `JSXText`.
   * When value is `false`, these nodes will be parsed as type `Literal`.
   */
  useJSXTextNode: false,

  // Cause the parser to error if it encounters an unknown AST node type (useful for testing)
  errorOnUnknownASTType: false,

  /*
   * Allows overriding of function used for logging.
   * When value is `false`, no logging will occur.
   * When value is not provided, `console.log()` will be used.
   */
  loggerFn: undefined
}
```

Example usage:

```js
const parser = require('@typescript-eslint/typescript-estree');
const code = `const hello: string = 'world';`;
const ast = parser.parse(code, {
  range: true,
  loc: true,
});
```

### version

Exposes the current version of typescript-estree as specified in package.json.

Example usage:

```js
const parser = require('@typescript-eslint/typescript-estree');
const version = parser.version;
```

### AST_NODE_TYPES

Exposes an object that contains the AST node types produced by the parser.

Example usage:

```js
const parser = require('@typescript-eslint/typescript-estree');
const astNodeTypes = parser.AST_NODE_TYPES;
```

## Supported TypeScript Version

We will always endeavor to support the latest stable version of TypeScript.

The version of TypeScript currently supported by this parser is `~3.2.1`. This is reflected in the `devDependency` requirement within the package.json file, and it is what the tests will be run against. We have an open `peerDependency` requirement in order to allow for experimentation on newer/beta versions of TypeScript.

If you use a non-supported version of TypeScript, the parser will log a warning to the console.

**Please ensure that you are using a supported version before submitting any issues/bug reports.**

## Reporting Issues

Please check the current list of open and known issues and ensure the issue has not been reported before. When creating a new issue provide as much information about your environment as possible. This includes:

- TypeScript version
- The `typescript-estree` version

## AST Alignment Tests

A couple of years after work on this parser began, the TypeScript Team at Microsoft began [officially supporting TypeScript parsing via Babel](https://blogs.msdn.microsoft.com/typescript/2018/08/27/typescript-and-babel-7/).

I work closely with TypeScript Team and we are gradually aliging the AST of this project with the one produced by Babel's parser. To that end, I have created a full test harness to compare the ASTs of the two projects which runs on every PR, please see the code for more details.

## Build/Test Commands

- `npm test` - run all tests
- `npm run unit-tests` - run only unit tests
- `npm run ast-alignment-tests` - run only Babylon AST alignment tests

## License

TypeScript ESTree inherits from the the original TypeScript ESLint Parser license, as the majority of the work began there. It is licensed under a permissive BSD 2-clause license.
