> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/init-declarations** for documentation.

## Rule Details

This rule extends the base [`eslint/init-declarations`](https://eslint.org/docs/rules/init-declarations) rule.
It adds support for TypeScript's `declare` variables.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "init-declarations": "off",
  "@typescript-eslint/init-declarations": ["error"]
}
```

## Options

See [`eslint/init-declarations` options](https://eslint.org/docs/rules/init-declarations#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/init-declarations.md)

</sup>
