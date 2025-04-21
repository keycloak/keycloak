> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/await-thenable** for documentation.

This rule disallows awaiting a value that is not a "Thenable" (an object which has `then` method, such as a Promise).
While it is valid JavaScript to await a non-`Promise`-like value (it will resolve immediately), this pattern is often a programmer error, such as forgetting to add parenthesis to call a function that returns a Promise.

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
await 'value';

const createValue = () => 'value';
await createValue();
```

### ✅ Correct

```ts
await Promise.resolve('value');

const createValue = async () => 'value';
await createValue();
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/await-thenable": "error"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you want to allow code to `await` non-Promise values.
This is generally not preferred, but can sometimes be useful for visual consistency.
