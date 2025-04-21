> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/prefer-enum-initializers** for documentation.

This rule recommends having each `enum`s member value explicitly initialized.

`enum`s are a practical way to organize semantically related constant values. However, by implicitly defining values, `enum`s can lead to unexpected bugs if it's modified without paying attention to the order of its items.

## Rule Details

`enum`s infers sequential numbers automatically when initializers are omitted:

```ts
enum Status {
  Open, // infer 0
  Closed, // infer 1
}
```

If a new member is added to the top of `Status`, both `Open` and `Closed` would have its values altered:

```ts
enum Status {
  Pending, // infer 0
  Open, // infer 1
  Closed, // infer 2
}
```

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
enum Status {
  Open = 1,
  Close,
}

enum Direction {
  Up,
  Down,
}

enum Color {
  Red,
  Green = 'Green'
  Blue = 'Blue',
}
```

### ✅ Correct

```ts
enum Status {
  Open = 'Open',
  Close = 'Close',
}

enum Direction {
  Up = 1,
  Down = 2,
}

enum Color {
  Red = 'Red',
  Green = 'Green',
  Blue = 'Blue',
}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/prefer-enum-initializers": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you don't care about `enum`s having implicit values you can safely disable this rule.
