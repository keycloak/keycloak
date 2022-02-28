# Enforces the use of `as Type` assertions instead of `<Type>` assertions (no-angle-bracket-type-assertion)

TypeScript disallows the use of `<Type>` assertions in `.tsx` because of the similarity with  
JSX's syntax, which makes it impossible to parse.

## Rule Details

This rule aims to standardise the use of type assertion style across the codebase

The following patterns are considered warnings:

```ts
const foo = <Foo>bar;
```

The following patterns are not warnings:

```ts
const foo = bar as Foo;
```

## When Not To Use It

If your codebase does not include `.tsx` files, then you will not need this rule.

## Further Reading

- [Typescript and JSX](https://www.typescriptlang.org/docs/handbook/jsx.html)

## Compatibility

- TSLint: [no-angle-bracket-type-assertion](https://palantir.github.io/tslint/rules/no-angle-bracket-type-assertion/)
