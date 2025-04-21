> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-duplicate-imports** for documentation.

## DEPRECATED

This rule has been deprecated in favour of the [`import/no-duplicates`](https://github.com/import-js/eslint-plugin-import/blob/HEAD/docs/rules/no-duplicates.md) rule.

## Rule Details

This rule extends the base [`eslint/no-duplicate-imports`](https://eslint.org/docs/rules/no-duplicate-imports) rule.
This version adds support for type-only import and export.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "no-duplicate-imports": "off",
  "@typescript-eslint/no-duplicate-imports": ["error"]
}
```

## Options

See [`eslint/no-duplicate-imports` options](https://eslint.org/docs/rules/no-duplicate-imports#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/no-duplicate-imports.md)

</sup>
