> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/consistent-type-definitions** for documentation.

There are two ways to define a type.

```ts
// type alias
type T1 = {
  a: string;
  b: number;
};

// interface keyword
interface T2 {
  a: string;
  b: number;
}
```

## Options

This rule accepts one string option:

- `"interface"`: enforce using `interface`s for object type definitions.
- `"type"`: enforce using `type`s for object type definitions.

For example:

```jsonc
{
  // Use type for object definitions
  "@typescript-eslint/consistent-type-definitions": ["error", "type"]
}
```

### `interface`

Examples of code with `interface` option.

<!--tabs-->

#### ❌ Incorrect

```ts
type T = { x: number };
```

#### ✅ Correct

```ts
type T = string;
type Foo = string | {};

interface T {
  x: number;
}
```

### `type`

Examples of code with `type` option.

<!--tabs-->

#### ❌ Incorrect

```ts
interface T {
  x: number;
}
```

#### ✅ Correct

```ts
type T = { x: number };
```

## When Not To Use It

If you specifically want to use an interface or type literal for stylistic reasons, you can disable this rule.
