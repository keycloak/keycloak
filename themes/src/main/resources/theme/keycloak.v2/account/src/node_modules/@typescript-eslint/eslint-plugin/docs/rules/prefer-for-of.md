> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/prefer-for-of** for documentation.

This rule recommends a for-of loop when the loop index is only used to read from an array that is being iterated.

## Rule Details

For cases where the index is only used to read from the array being iterated, a for-of loop is easier to read and write.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```js
for (let i = 0; i < arr.length; i++) {
  console.log(arr[i]);
}
```

### ✅ Correct

```js
for (const x of arr) {
  console.log(x);
}

for (let i = 0; i < arr.length; i++) {
  // i is used to write to arr, so for-of could not be used.
  arr[i] = 0;
}

for (let i = 0; i < arr.length; i++) {
  // i is used independent of arr, so for-of could not be used.
  console.log(i, arr[i]);
}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/prefer-for-of": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you transpile for browsers that do not support for-of loops, you may wish to use traditional for loops that produce more compact code.
