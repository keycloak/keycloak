> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/unified-signatures** for documentation.

## Rule Details

This rule aims to keep the source code as maintainable as possible by reducing the amount of overloads.

## Options

```ts
type Options = {
  ignoreDifferentlyNamedParameters?: boolean;
};

const defaultOptions: Options = {
  ignoreDifferentlyNamedParameters: false,
};
```

The rule accepts an options object with the following property:

- `ignoreDifferentlyNamedParameters`: whether two parameters with different names at the same index should be considered different even if their types are the same.

Examples of code for this rule with the default options:

<!--tabs-->

### ❌ Incorrect

```ts
function x(x: number): void;
function x(x: string): void;
```

```ts
function y(): void;
function y(...x: number[]): void;
```

### ✅ Correct

```ts
function x(x: number | string): void;
```

```ts
function y(...x: number[]): void;
```

Examples of code for this rule with `ignoreDifferentlyNamedParameters`:

<!--tabs-->

### ❌ Incorrect

```ts
function f(a: number): void;
function f(a: string): void;
```

```ts
function f(...a: number[]): void;
function f(...b: string[]): void;
```

### ✅ Correct

```ts
function f(a: number): void;
function f(b: string): void;
```

```ts
function f(...a: number[]): void;
function f(...a: string[]): void;
```
