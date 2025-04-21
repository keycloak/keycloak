> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-unnecessary-type-arguments** for documentation.

Warns if an explicitly specified type argument is the default for that type parameter.

## Rule Details

Type parameters in TypeScript may specify a default value.
For example:

```ts
function f<T = number>() {}
```

It is redundant to provide an explicit type parameter equal to that default.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
function f<T = number>() {}
f<number>();

function g<T = number, U = string>() {}
g<string, string>();

class C<T = number> {}
function h(c: C<number>) {}
new C<number>();
class D extends C<number> {}

interface I<T = number> {}
class Impl implements I<number> {}
```

### ✅ Correct

```ts
function f<T = number>() {}
f<string>();

function g<T = number, U = string>() {}
g<number, number>();

class C<T = number> {}
new C<string>();
class D extends C<string> {}

interface I<T = number> {}
class Impl implements I<string> {}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-unnecessary-type-arguments": "warn"
  }
}
```

This rule is not configurable.
