> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/prefer-string-starts-ends-with** for documentation.

There are multiple ways to verify if a string starts or ends with a specific string, such as `foo.indexOf('bar') === 0`.
Since ES2015 has added `String#startsWith` and `String#endsWith`, this rule reports other ways to be consistent.

## Rule Details

This rule is aimed at enforcing a consistent way to check whether a string starts or ends with a specific string.

Examples of code for this rule:

<!--tabs-->

### ❌ Incorrect

```ts
let foo: string;

// starts with
foo[0] === 'b';
foo.charAt(0) === 'b';
foo.indexOf('bar') === 0;
foo.slice(0, 3) === 'bar';
foo.substring(0, 3) === 'bar';
foo.match(/^bar/) != null;
/^bar/.test(foo);

// ends with
foo[foo.length - 1] === 'b';
foo.charAt(foo.length - 1) === 'b';
foo.lastIndexOf('bar') === foo.length - 3;
foo.slice(-3) === 'bar';
foo.substring(foo.length - 3) === 'bar';
foo.match(/bar$/) != null;
/bar$/.test(foo);
```

### ✅ Correct

```ts
foo.startsWith('bar');
foo.endsWith('bar');
```

## Options

```jsonc
// .eslintrc.json
{
  "rules": {
    "@typescript-eslint/prefer-string-starts-ends-with": "warn"
  }
}
```

This rule is not configurable.

## When Not To Use It

If you don't mind that style, you can turn this rule off safely.
