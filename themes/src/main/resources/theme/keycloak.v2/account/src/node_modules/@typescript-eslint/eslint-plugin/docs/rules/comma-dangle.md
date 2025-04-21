> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/comma-dangle** for documentation.

## Rule Details

This rule extends the base [`eslint/comma-dangle`](https://eslint.org/docs/rules/comma-dangle) rule.
It adds support for TypeScript syntax.

See the [ESLint documentation](https://eslint.org/docs/rules/comma-dangle) for more details on the `comma-dangle` rule.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "comma-dangle": "off",
  "@typescript-eslint/comma-dangle": ["error"]
}
```

In addition to the options supported by the `comma-dangle` rule in ESLint core, the rule adds the following options:

## Options

This rule has a string option and an object option.

- Object option:

  - `"enums"` is for trailing comma in enum. (e.g. `enum Foo = {Bar,}`)
  - `"generics"` is for trailing comma in generic. (e.g. `function foo<T,>() {}`)
  - `"tuples"` is for trailing comma in tuple. (e.g. `type Foo = [string,]`)

- [See the other options allowed](https://eslint.org/docs/rules/comma-dangle#options)

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/comma-dangle.md)

</sup>
