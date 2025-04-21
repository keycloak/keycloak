> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/non-nullable-type-assertion-style** for documentation.

This rule detects when an `as` cast is doing the same job as a `!` would, and suggests fixing the code to be an `!`.

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
const maybe = Math.random() > 0.5 ? '' : undefined;

const definitely = maybe as string;
const alsoDefinitely = <string>maybe;
```

### ✅ Correct

```ts
const maybe = Math.random() > 0.5 ? '' : undefined;

const definitely = maybe!;
const alsoDefinitely = maybe!;
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/non-nullable-type-assertion-style": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you don't mind having unnecessarily verbose type casts, you can avoid this rule.
