> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-var-requires** for documentation.

In other words, the use of forms such as `var foo = require("foo")` are banned. Instead use ES6 style imports or `import foo = require("foo")` imports.

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
var foo = require('foo');
const foo = require('foo');
let foo = require('foo');
```

### ✅ Correct

```ts
import foo = require('foo');
require('foo');
import foo from 'foo';
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-var-requires": "error"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you don't care about TypeScript module syntax, then you will not need this rule.
