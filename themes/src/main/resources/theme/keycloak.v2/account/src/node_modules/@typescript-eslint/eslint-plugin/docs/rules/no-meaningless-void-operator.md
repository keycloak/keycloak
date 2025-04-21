> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-meaningless-void-operator** for documentation.

Disallows the `void` operator when its argument is already of type `void` or `undefined`.

## Rule Details

The `void` operator is a useful tool to convey the programmer's intent to discard a value. For example, it is recommended as one way of suppressing [`@typescript-eslint/no-floating-promises`](./no-floating-promises.md) instead of adding `.catch()` to a promise.

This rule helps an author catch API changes where previously a value was being discarded at a call site, but the callee changed so it no longer returns a value. When combined with [no-unused-expressions](https://eslint.org/docs/rules/no-unused-expressions), it also helps _readers_ of the code by ensuring consistency: a statement that looks like `void foo();` is **always** discarding a return value, and a statement that looks like `foo();` is **never** discarding a return value.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
void (() => {})();

function foo() {}
void foo();
```

### ✅ Correct

```ts
(() => {})();

function foo() {}
foo(); // nothing to discard

function bar(x: number) {
  void x; // discarding a number
  return 2;
}
void bar(); // discarding a number
```

## Options

This rule accepts a single object option with the following default configuration:

```json
{
  "@typescript-eslint/no-meaningless-void-operator": [
    "error",
    {
      "checkNever": false
    }
  ]
}
```

- `checkNever: true` will suggest removing `void` when the argument has type `never`.
