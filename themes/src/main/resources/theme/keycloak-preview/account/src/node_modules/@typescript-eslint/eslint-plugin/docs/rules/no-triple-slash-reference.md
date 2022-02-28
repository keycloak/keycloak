# Disallow `/// <reference path="" />` comments (no-triple-slash-reference)

Triple-slash reference directive comments should not be used anymore. Use `import` instead.

Before TypeScript adopted ES6 Module syntax,
triple-slash reference directives were used to specify dependencies.
Now that we have `import`, triple-slash reference directives are discouraged for specifying dependencies
in favor of `import`.

A triple-slash reference directive is a comment beginning with three slashes followed by a path to the module being imported:
`/// <reference path="./Animal" />`.
ES6 Modules handle this now:
`import animal from "./Animal"`

## Rule Details

Does not allow the use of `/// <reference />` comments.

The following patterns are considered warnings:

```ts
/// <reference path="Animal">
```

The following patterns are not warnings:

```ts
import Animal from 'Animal';
```

## When Not To Use It

If you use `/// <reference />` style imports.

## Further Reading

- TypeScript [Triple-Slash Directives](https://www.typescriptlang.org/docs/handbook/triple-slash-directives.html)

## Compatibility

- TSLint: [no-reference](http://palantir.github.io/tslint/rules/no-reference/)
