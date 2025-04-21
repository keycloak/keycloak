> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-misused-new** for documentation.

Warns on apparent attempts to define constructors for interfaces or `new` for classes.

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
class C {
  new(): C;
}

interface I {
  new (): I;
  constructor(): void;
}
```

### ✅ Correct

```ts
class C {
  constructor() {}
}
interface I {
  new (): C;
}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-misused-new": "error"
  }
}
```

This rule is not configurable.
