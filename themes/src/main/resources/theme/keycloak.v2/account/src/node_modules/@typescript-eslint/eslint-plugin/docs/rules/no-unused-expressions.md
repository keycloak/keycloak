> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-unused-expressions** for documentation.

## Rule Details

This rule extends the base [`eslint/no-unused-expressions`](https://eslint.org/docs/rules/no-unused-expressions) rule.
It adds support for optional call expressions `x?.()`, and directive in module declarations.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "no-unused-expressions": "off",
  "@typescript-eslint/no-unused-expressions": ["error"]
}
```

## Options

See [`eslint/no-unused-expressions` options](https://eslint.org/docs/rules/no-unused-expressions#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/no-unused-expressions.md)

</sup>
