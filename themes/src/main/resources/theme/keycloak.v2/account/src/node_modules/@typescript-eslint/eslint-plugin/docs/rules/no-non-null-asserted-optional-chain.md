> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-non-null-asserted-optional-chain** for documentation.

## Rule Details

Optional chain expressions are designed to return `undefined` if the optional property is nullish.
Using non-null assertions after an optional chain expression is wrong, and introduces a serious type safety hole into your code.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
/* eslint @typescript-eslint/no-non-null-asserted-optional-chain: "error" */

foo?.bar!;
foo?.bar()!;

// Prior to TS3.9, foo?.bar!.baz meant (foo?.bar).baz - i.e. the non-null assertion is applied to the entire chain so far.
// For TS3.9 and greater, the non-null assertion is only applied to the property itself, so it's safe.
// The following is incorrect code if you're using less than TS3.9
foo?.bar!.baz;
foo?.bar!();
foo?.bar!().baz;
```

### ✅ Correct

```ts
/* eslint @typescript-eslint/no-non-null-asserted-optional-chain: "error" */

foo?.bar;
(foo?.bar).baz;
foo?.bar();
foo?.bar();
foo?.bar().baz;

// The following is correct code if you're using TS3.9 or greater
foo?.bar!.baz;
foo?.bar!();
foo?.bar!().baz;
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-non-null-asserted-optional-chain": "error"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you are not using TypeScript 3.7 (or greater), then you will not need to use this rule, as the operator is not supported.

## Further Reading

- [TypeScript 3.7 Release Notes](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-3-7.html)
- [Optional Chaining Proposal](https://github.com/tc39/proposal-optional-chaining/)
