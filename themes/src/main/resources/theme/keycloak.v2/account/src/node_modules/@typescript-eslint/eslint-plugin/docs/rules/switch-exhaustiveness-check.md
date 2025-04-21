> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/switch-exhaustiveness-check** for documentation.

Union type may have a lot of parts. It's easy to forget to consider all cases in switch. This rule reminds which parts are missing. If domain of the problem requires to have only a partial switch, developer may _explicitly_ add a default clause.

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
type Day =
  | 'Monday'
  | 'Tuesday'
  | 'Wednesday'
  | 'Thursday'
  | 'Friday'
  | 'Saturday'
  | 'Sunday';

const day = 'Monday' as Day;
let result = 0;

switch (day) {
  case 'Monday': {
    result = 1;
    break;
  }
}
```

### ✅ Correct

```ts
type Day =
  | 'Monday'
  | 'Tuesday'
  | 'Wednesday'
  | 'Thursday'
  | 'Friday'
  | 'Saturday'
  | 'Sunday';

const day = 'Monday' as Day;
let result = 0;

switch (day) {
  case 'Monday': {
    result = 1;
    break;
  }
  case 'Tuesday': {
    result = 2;
    break;
  }
  case 'Wednesday': {
    result = 3;
    break;
  }
  case 'Thursday': {
    result = 4;
    break;
  }
  case 'Friday': {
    result = 5;
    break;
  }
  case 'Saturday': {
    result = 6;
    break;
  }
  case 'Sunday': {
    result = 7;
    break;
  }
}
```

### ✅ Correct

```ts
type Day =
  | 'Monday'
  | 'Tuesday'
  | 'Wednesday'
  | 'Thursday'
  | 'Friday'
  | 'Saturday'
  | 'Sunday';

const day = 'Monday' as Day;
let result = 0;

switch (day) {
  case 'Monday': {
    result = 1;
    break;
  }
  default: {
    result = 42;
  }
}
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/switch-exhaustiveness-check": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If program doesn't have union types with many parts. Downside of this rule is the need for type information, so it's slower than regular rules.
