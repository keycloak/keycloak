> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-useless-empty-export** for documentation.

## Rule Details

An empty `export {}` statement is sometimes useful in TypeScript code to turn a file that would otherwise be a script file into a module file.
Per the TypeScript Handbook [Modules](https://www.typescriptlang.org/docs/handbook/modules.html) page:

> In TypeScript, just as in ECMAScript 2015, any file containing a top-level import or export is considered a module.
> Conversely, a file without any top-level import or export declarations is treated as a script whose contents are available in the global scope (and therefore to modules as well).

However, an `export {}` statement does nothing if there are any other top-level import or export statements in a file.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
export const value = 'Hello, world!';
export {};
```

```ts
import 'some-other-module';
export {};
```

### ✅ Correct

```ts
export const value = 'Hello, world!';
```

```ts
import 'some-other-module';
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-useless-empty-export": "warn"
  }
}
```

This rule is not configurable.
