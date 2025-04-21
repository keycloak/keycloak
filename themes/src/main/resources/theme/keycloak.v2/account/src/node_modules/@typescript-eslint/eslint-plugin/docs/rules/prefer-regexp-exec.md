> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/prefer-regexp-exec** for documentation.

As `String#match` is defined to be the same as `RegExp#exec` when the regular expression does not include the `g` flag, prefer a consistent usage.

## Rule Details

This rule is aimed at enforcing a consistent way to apply regular expressions to strings.

From [`String#match` on MDN](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/match):

> If the regular expression does not include the g flag, returns the same result as `RegExp.exec()`.

`RegExp#exec` may also be slightly faster than `String#match`; this is the reason to choose it as the preferred usage.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
'something'.match(/thing/);

'some things are just things'.match(/thing/);

const text = 'something';
const search = /thing/;
text.match(search);
```

### ✅ Correct

```ts
/thing/.exec('something');

'some things are just things'.match(/thing/g);

const text = 'something';
const search = /thing/;
search.exec(text);
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/prefer-regexp-exec": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you prefer consistent use of `String#match` for both, with `g` flag and without it, you can turn this rule off.
