> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-require-imports** for documentation.

Prefer the newer ES6-style imports over `require()`.

## Rule Details

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
var lib = require('lib');
let lib2 = require('lib2');
var lib5 = require('lib5'),
  lib6 = require('lib6');
import lib8 = require('lib8');
```

### ✅ Correct

```ts
import { l } from 'lib';
var lib3 = load('not_an_import');
var lib4 = lib2.subImport;
var lib7 = 700;
import lib9 = lib2.anotherSubImport;
import lib10 from 'lib10';
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/no-require-imports": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you don't care about TypeScript module syntax, then you will not need this rule.
