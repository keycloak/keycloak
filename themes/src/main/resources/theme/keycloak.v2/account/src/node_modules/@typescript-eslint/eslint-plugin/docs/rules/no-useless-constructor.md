> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-useless-constructor** for documentation.

## Rule Details

This rule extends the base [`eslint/no-useless-constructor`](https://eslint.org/docs/rules/no-useless-constructor) rule.
It adds support for:

- constructors marked as `protected` / `private` (i.e. marking a constructor as non-public),
- `public` constructors when there is no superclass,
- constructors with only parameter properties.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "no-useless-constructor": "off",
  "@typescript-eslint/no-useless-constructor": ["error"]
}
```

## Options

See [`eslint/no-useless-constructor` options](https://eslint.org/docs/rules/no-useless-constructor#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/no-useless-constructor.md)

</sup>

## Caveat

This lint rule will report on constructors whose sole purpose is to change visibility of a parent constructor.
See [discussion on this rule's lack of type information](https://github.com/typescript-eslint/typescript-eslint/issues/3820#issuecomment-917821240) for context.
