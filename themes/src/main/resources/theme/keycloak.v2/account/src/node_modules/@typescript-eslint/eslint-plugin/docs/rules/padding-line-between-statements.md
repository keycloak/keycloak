> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/padding-line-between-statements** for documentation.

## Rule Details

This rule extends the base [`eslint/padding-line-between-statements`](https://eslint.org/docs/rules/padding-line-between-statements) rule.

**It adds support for TypeScript constructs such as `interface` and `type`.**

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "padding-line-between-statements": "off",
  "@typescript-eslint/padding-line-between-statements": [
    "error",
    {
      "blankLine": "always",
      "prev": "var",
      "next": "return"
    }
  ]
}
```

```jsonc
{
  // Example - Add blank lines before interface and type definitions.
  // note you must disable the base rule as it can report incorrect errors
  "padding-line-between-statements": "off",
  "@typescript-eslint/padding-line-between-statements": [
    "error",
    {
      "blankLine": "always",
      "prev": "*",
      "next": ["interface", "type"]
    }
  ]
}
```

## Options

See [`eslint/padding-line-between-statements` options](https://eslint.org/docs/rules/padding-line-between-statements#options).

In addition to options provided by ESLint, `interface` and `type` can be used as statement types.

**Note:** ESLint `cjs-export` and `cjs-import` statement types are renamed to `exports` and `require` respectively.

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/padding-line-between-statements.md)

</sup>
