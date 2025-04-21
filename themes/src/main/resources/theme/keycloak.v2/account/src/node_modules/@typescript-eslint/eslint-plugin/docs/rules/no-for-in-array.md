> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-for-in-array** for documentation.

This rule prohibits iterating over an array with a for-in loop.

## Rule Details

A for-in loop (`for (var k in o)`) iterates over the properties of an Object.
While it is legal to use for-in loops with array types, it is not common.
for-in will iterate over the indices of the array as strings, omitting any "holes" in
the array.
More common is to use for-of, which iterates over the values of an array.
If you want to iterate over the indices, alternatives include:

```js
array.forEach((value, index) => { ... });
for (const [index, value] of array.entries()) { ... }
for (let i = 0; i < array.length; i++) { ... }
```

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```js
for (const x in [3, 4, 5]) {
  console.log(x);
}
```

### ✅ Correct

```js
for (const x in { a: 3, b: 4, c: 5 }) {
  console.log(x);
}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-for-in-array": "error"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you want to iterate through a loop using the indices in an array as strings, you can turn off this rule.
