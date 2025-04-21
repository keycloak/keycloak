> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/space-infix-ops** for documentation.

This rule extends the base [`eslint/space-infix-ops`](https://eslint.org/docs/rules/space-infix-ops) rule.

It also add support for enum members

```ts
enum MyEnum {
  KEY = 'value',
}
```

## How to Use

```jsonc
{
  "space-infix-ops": "off",
  "@typescript-eslint/space-infix-ops": ["error", { "int32Hint": false }]
}
```

## Options

See [`eslint/space-infix-ops` options](https://eslint.org/docs/rules/space-infix-ops#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/main/docs/rules/space-infix-ops.md)

</sup>
