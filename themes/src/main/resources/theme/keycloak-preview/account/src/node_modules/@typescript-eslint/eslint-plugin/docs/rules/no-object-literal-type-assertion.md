# Forbids an object literal to appear in a type assertion expression (no-object-literal-type-assertion)

Always prefer `const x: T = { ... };` to `const x = { ... } as T;`. Casting to `any` and `unknown` is still allowed.

## Rule Details

Examples of **incorrect** code for this rule.

```ts
const x = { ... } as T;
```

Examples of **correct** code for this rule.

```ts
const x: T = { ... };
const y = { ... } as any;
const z = { ... } as unknown;
```

## Options

```cjson
{
  "@typescript-eslint/no-object-literal-type-assertion": ["error", {
    allowAsParameter: false // Allow type assertion in call and new expression, default false
  }]
}
```

## Compatibility

- TSLint: [no-object-literal-type-assertion](https://palantir.github.io/tslint/rules/no-object-literal-type-assertion/)
