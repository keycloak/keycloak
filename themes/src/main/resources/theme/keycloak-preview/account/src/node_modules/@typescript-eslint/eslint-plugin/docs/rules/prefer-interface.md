# Prefer an interface declaration over a type literal (type T = { ... }) (prefer-interface)

Interfaces are generally preferred over type literals because interfaces can be implemented, extended and merged.

## Rule Details

Examples of **incorrect** code for this rule.

```ts
type T = { x: number };
```

Examples of **correct** code for this rule.

```ts
type T = string;
type Foo = string | {};

interface T {
  x: number;
}
```

## Options

```CJSON
{
    "interface-over-type-literal": "error"
}
```

## Compatibility

- TSLint: [interface-over-type-literal](https://palantir.github.io/tslint/rules/interface-over-type-literal/)
