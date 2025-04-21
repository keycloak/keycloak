> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/space-before-blocks** for documentation.

## Rule Details

This rule extends the base [`eslint/space-before-blocks`](https://eslint.org/docs/rules/space-before-blocks) rule.
It adds support for interfaces and enums:

### ❌ Incorrect

```ts
enum Breakpoint{
  Large, Medium;
}

interface State{
  currentBreakpoint: Breakpoint;
}
```

### ✅ Correct

```ts
enum Breakpoint {
  Large, Medium;
}

interface State {
  currentBreakpoint: Breakpoint;
}
```

In case a more specific options object is passed these blocks will follow `classes` configuration option.

## How to Use

```jsonc
{
  // note you must disable the base rule as it can report incorrect errors
  "space-before-blocks": "off",
  "@typescript-eslint/space-before-blocks": ["error"]
}
```

## Options

See [`eslint/space-before-blocks` options](https://eslint.org/docs/rules/space-before-blocks#options).

<sup>

Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/master/docs/rules/space-before-blocks.md)

</sup>
