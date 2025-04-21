> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/space-before-function-paren** for documentation.

## Rule Details

This rule extends the base [`eslint/space-before-function-paren`](https://eslint.org/docs/rules/space-before-function-paren) rule.
It adds support for generic type parameters on function calls.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "space-before-function-paren": "off",
  "@typescript-eslint/space-before-function-paren": ["error"]
}
```

## Options

See [`eslint/space-before-function-paren` options](https://eslint.org/docs/rules/space-before-function-paren#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/space-before-function-paren.md)

</sup>
